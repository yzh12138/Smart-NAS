package yzh.stock.business.config;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import yzh.stock.business.entity.SysUser;
import yzh.stock.business.service.SysUserService;
import yzh.stock.business.utils.PasswordUtil;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * WebDAV Filter - 处理 PROPFIND, MKCOL, MOVE, COPY 等 WebDAV 方法
 */
@Component
public class WebDavFilter implements Filter {

    @Value("${photo.storage.base-path:D:\\test\\photos}")
    private String storageBasePath;

    private final SysUserService userService;

    public WebDavFilter(SysUserService userService) {
        this.userService = userService;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String uri = request.getRequestURI();

        // 只处理 /webdav/ 路径
        if (!uri.startsWith("/webdav")) {
            chain.doFilter(req, res);
            return;
        }

        String method = request.getMethod().toUpperCase();

        try {
            // 认证检查（OPTIONS 不需要认证）
            if (!"OPTIONS".equals(method)) {
                if (!checkAuth(request, response)) return;
            }

            // 处理 WebDAV 专有方法
            switch (method) {
                case "PROPFIND":
                    handlePropfind(request, response);
                    break;
                case "MKCOL":
                    handleMkcol(request, response);
                    break;
                case "MOVE":
                    handleMove(request, response);
                    break;
                case "COPY":
                    handleCopy(request, response);
                    break;
                case "OPTIONS":
                    handleOptions(response);
                    break;
                case "GET":
                    handleGet(request, response);
                    break;
                case "PUT":
                    handlePut(request, response);
                    break;
                case "DELETE":
                    handleDelete(request, response);
                    break;
                default:
                    chain.doFilter(req, res);
                    break;
            }
        } catch (Exception e) {
            response.setStatus(500);
            response.setContentType("text/plain; charset=utf-8");
            response.getWriter().write("WebDAV Error: " + e.getMessage());
        }
    }

    private boolean checkAuth(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            response.setHeader("WWW-Authenticate", "Basic realm=\"Smart-NAS WebDAV\"");
            response.setStatus(401);
            response.getWriter().write("Unauthorized");
            return false;
        }

        try {
            String decoded = new String(Base64.getDecoder().decode(authHeader.substring(6)), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 2);
            if (parts.length != 2) {
                response.setStatus(401);
                return false;
            }

            SysUser user = userService.findByUsername(parts[0]);
            if (user == null || !PasswordUtil.matches(parts[1], user.getPassword())) {
                response.setStatus(401);
                return false;
            }
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    private void handleOptions(HttpServletResponse response) {
        response.setHeader("DAV", "1, 2");
        response.setHeader("Allow", "GET, PUT, DELETE, MKCOL, PROPFIND, PROPPATCH, COPY, MOVE, OPTIONS");
        response.setStatus(200);
    }

    private void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = extractPath(request);
        File file = resolvePath(path);

        if (!file.exists() || !file.isFile()) {
            response.setStatus(404);
            return;
        }

        response.setContentLengthLong(file.length());
        response.setContentType(getContentType(file.getName()));

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                response.getOutputStream().write(buffer, 0, bytesRead);
            }
        }
    }

    private void handlePut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = extractPath(request);
        File file = resolvePath(path);

        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (InputStream is = request.getInputStream();
             FileOutputStream fos = new FileOutputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        response.setStatus(file.exists() ? 201 : 204);
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = extractPath(request);
        File file = resolvePath(path);

        if (!file.exists()) {
            response.setStatus(404);
            return;
        }

        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            file.delete();
        }

        response.setStatus(204);
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }

    private String getContentType(String filename) {
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.') + 1).toLowerCase() : "";
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "heic", "heif" -> "image/heic";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "mp3" -> "audio/mpeg";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    private void handlePropfind(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = extractPath(request);
        File dir = resolvePath(path);

        if (!dir.exists()) {
            response.setStatus(404);
            return;
        }

        int depth = parseDepth(request);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        xml.append("<D:multistatus xmlns:D=\"DAV:\">");

        if (dir.isDirectory()) {
            appendDirectoryProps(xml, dir, path, depth);
        } else {
            appendFileProps(xml, dir, path);
        }

        xml.append("</D:multistatus>");

        response.setStatus(207);
        response.setContentType("application/xml; charset=utf-8");
        response.getWriter().write(xml.toString());
    }

    private void handleMkcol(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = extractPath(request);
        File dir = resolvePath(path);

        if (dir.exists()) {
            response.setStatus(405);
            return;
        }

        dir.mkdirs();
        response.setStatus(201);
    }

    private void handleMove(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String destHeader = request.getHeader("Destination");
        if (destHeader == null) {
            response.setStatus(400);
            return;
        }

        String sourcePath = extractPath(request);
        String destPath = URLDecoder.decode(destHeader.replaceFirst("/webdav", ""), StandardCharsets.UTF_8);

        File source = resolvePath(sourcePath);
        File dest = resolvePath(destPath);

        if (!source.exists()) {
            response.setStatus(404);
            return;
        }

        dest.getParentFile().mkdirs();
        if (source.renameTo(dest)) {
            response.setStatus(201);
        } else {
            response.setStatus(500);
        }
    }

    private void handleCopy(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String destHeader = request.getHeader("Destination");
        if (destHeader == null) {
            response.setStatus(400);
            return;
        }

        String sourcePath = extractPath(request);
        String destPath = URLDecoder.decode(destHeader.replaceFirst("/webdav", ""), StandardCharsets.UTF_8);

        File source = resolvePath(sourcePath);
        File dest = resolvePath(destPath);

        if (!source.exists()) {
            response.setStatus(404);
            return;
        }

        dest.getParentFile().mkdirs();
        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        response.setStatus(204);
    }

    // ==================== 工具方法 ====================

    private String extractPath(HttpServletRequest request) {
        String fullPath = request.getRequestURI();
        String path = fullPath.replaceFirst("/webdav", "");
        if (path.isEmpty()) path = "/";
        try {
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) { /* ignore */ }
        return path;
    }

    private File resolvePath(String webDavPath) {
        String normalized = webDavPath.replace("\\", "/");
        if (normalized.contains("..")) normalized = "/";
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        return Paths.get(storageBasePath, "webdav", normalized).toFile();
    }

    private int parseDepth(HttpServletRequest request) {
        String depth = request.getHeader("Depth");
        if (depth == null || "infinity".equalsIgnoreCase(depth)) return -1;
        try { return Integer.parseInt(depth); } catch (NumberFormatException e) { return 1; }
    }

    private void appendDirectoryProps(StringBuilder xml, File dir, String webDavPath, int depth) {
        String href = "/webdav" + webDavPath;
        if (!href.endsWith("/")) href += "/";
        appendPropResponse(xml, href, true, dir.length(), dir.lastModified());

        if (depth != 0 && dir.listFiles() != null) {
            for (File child : dir.listFiles()) {
                String childPath = webDavPath + "/" + child.getName();
                if (!childPath.startsWith("/")) childPath = "/" + childPath;
                if (child.isDirectory()) {
                    appendDirectoryProps(xml, child, childPath, depth - 1);
                } else {
                    appendFileProps(xml, child, childPath);
                }
            }
        }
    }

    private void appendFileProps(StringBuilder xml, File file, String webDavPath) {
        String href = "/webdav" + webDavPath;
        appendPropResponse(xml, href, false, file.length(), file.lastModified());
    }

    private void appendPropResponse(StringBuilder xml, String href, boolean isCollection, long size, long lastModified) {
        xml.append("<D:response>");
        xml.append("<D:href>").append(escapeXml(href)).append("</D:href>");
        xml.append("<D:propstat><D:prop>");
        xml.append("<D:resourcetype>").append(isCollection ? "<D:collection/>" : "").append("</D:resourcetype>");
        xml.append("<D:getcontentlength>").append(size).append("</D:getcontentlength>");
        xml.append("<D:getlastmodified>").append(formatDate(lastModified)).append("</D:getlastmodified>");
        xml.append("<D:creationdate>").append(formatDateIso(lastModified)).append("</D:creationdate>");
        xml.append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat>");
        xml.append("</D:response>");
    }

    private String formatDate(long timestamp) {
        return LocalDateTime.ofInstant(new Date(timestamp).toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US));
    }

    private String formatDateIso(long timestamp) {
        return LocalDateTime.ofInstant(new Date(timestamp).toInstant(), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
