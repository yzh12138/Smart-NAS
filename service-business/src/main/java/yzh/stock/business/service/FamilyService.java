package yzh.stock.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.stock.business.entity.Family;
import yzh.stock.business.entity.FamilyMember;
import yzh.stock.business.entity.FamilyMedia;
import yzh.stock.business.entity.Photo;
import yzh.stock.business.mapper.FamilyMapper;
import yzh.stock.business.mapper.FamilyMemberMapper;
import yzh.stock.business.mapper.FamilyMediaMapper;
import yzh.stock.business.mapper.PhotoMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FamilyService {

    private final FamilyMapper familyMapper;
    private final FamilyMemberMapper memberMapper;
    private final FamilyMediaMapper mediaMapper;
    private final PhotoMapper photoMapper;
    private final JdbcTemplate jdbcTemplate;

    public FamilyService(FamilyMapper familyMapper, FamilyMemberMapper memberMapper,
                         FamilyMediaMapper mediaMapper, PhotoMapper photoMapper, JdbcTemplate jdbcTemplate) {
        this.familyMapper = familyMapper;
        this.memberMapper = memberMapper;
        this.mediaMapper = mediaMapper;
        this.photoMapper = photoMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    // 家庭管理
    public Family createFamily(String name, Long ownerId, String description) {
        Family family = new Family();
        family.setFamilyName(name);
        family.setOwnerId(ownerId);
        family.setDescription(description);
        family.setFamilyCode(generateFamilyCode());
        family.setStatus(1);
        familyMapper.insert(family);
        // 创建者自动成为管理员
        addMember(family.getId(), ownerId, "admin", 1);
        return family;
    }

    private String generateFamilyCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < 6; j++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            String code = sb.toString();
            Long count = familyMapper.selectCount(
                    new LambdaQueryWrapper<Family>().eq(Family::getFamilyCode, code)
            );
            if (count == 0) return code;
        }
        return System.currentTimeMillis() % 1000000 + "";
    }

    public Family findByCode(String code) {
        return familyMapper.selectOne(
                new LambdaQueryWrapper<Family>()
                        .eq(Family::getFamilyCode, code)
                        .eq(Family::getStatus, 1)
        );
    }

    public void updateFamily(Long familyId, String name, String description) {
        Family family = familyMapper.selectById(familyId);
        if (family != null) {
            if (name != null) family.setFamilyName(name);
            if (description != null) family.setDescription(description);
            familyMapper.updateById(family);
        }
    }

    public void dissolveFamily(Long familyId) {
        Family family = familyMapper.selectById(familyId);
        if (family != null) {
            family.setStatus(0);
            familyMapper.updateById(family);
        }
    }

    public List<Family> getUserFamilies(Long userId) {
        // 获取用户作为成员的家庭
        List<FamilyMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getUserId, userId)
                        .eq(FamilyMember::getStatus, 1)
        );
        Set<Long> familyIds = new HashSet<>();
        List<Family> families = new ArrayList<>();
        for (FamilyMember m : memberships) {
            Family f = familyMapper.selectById(m.getFamilyId());
            if (f != null && f.getStatus() == 1) {
                families.add(f);
                familyIds.add(f.getId());
            }
        }
        // 获取用户创建但已无成员的家庭（仅创建者可见）
        List<Family> ownedFamilies = familyMapper.selectList(
                new LambdaQueryWrapper<Family>()
                        .eq(Family::getOwnerId, userId)
                        .eq(Family::getStatus, 1)
        );
        for (Family f : ownedFamilies) {
            if (!familyIds.contains(f.getId())) {
                List<FamilyMember> activeMembers = memberMapper.selectList(
                        new LambdaQueryWrapper<FamilyMember>()
                                .eq(FamilyMember::getFamilyId, f.getId())
                                .eq(FamilyMember::getStatus, 1)
                );
                if (activeMembers.isEmpty()) {
                    families.add(f);
                }
            }
        }
        return families;
    }

    public List<Family> getOwnedFamilies(Long userId) {
        return familyMapper.selectList(
                new LambdaQueryWrapper<Family>()
                        .eq(Family::getOwnerId, userId)
                        .eq(Family::getStatus, 1)
        );
    }

    public boolean isAdminOrOwner(Long familyId, Long userId) {
        Family family = familyMapper.selectById(familyId);
        if (family == null) return false;
        if (family.getOwnerId().equals(userId)) return true;
        FamilyMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId)
                        .eq(FamilyMember::getStatus, 1)
        );
        return member != null && "admin".equals(member.getRole());
    }

    // 成员管理
    public void addMember(Long familyId, Long userId, String role, int status) {
        FamilyMember existing = memberMapper.selectOne(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId)
        );
        if (existing != null) {
            existing.setStatus(status);
            if (status == 1) existing.setJoinTime(LocalDateTime.now());
            memberMapper.updateById(existing);
        } else {
            FamilyMember member = new FamilyMember();
            member.setFamilyId(familyId);
            member.setUserId(userId);
            member.setRole(role);
            member.setStatus(status);
            if (status == 1) member.setJoinTime(LocalDateTime.now());
            memberMapper.insert(member);
        }
    }

    public void approveMember(Long memberId) {
        FamilyMember member = memberMapper.selectById(memberId);
        if (member != null) {
            member.setStatus(1);
            member.setJoinTime(LocalDateTime.now());
            memberMapper.updateById(member);
        }
    }

    public void rejectMember(Long memberId) {
        FamilyMember member = memberMapper.selectById(memberId);
        if (member != null) {
            member.setStatus(2);
            memberMapper.updateById(member);
        }
    }

    public void removeMember(Long familyId, Long userId) {
        memberMapper.delete(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getUserId, userId)
        );
    }

    public List<FamilyMember> getMembers(Long familyId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
        );
    }

    public List<FamilyMember> getPendingMembers(Long familyId) {
        return memberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getFamilyId, familyId)
                        .eq(FamilyMember::getStatus, 0)
        );
    }

    // 媒体共享
    public void shareMedia(Long familyId, Long photoId, Long userId) {
        FamilyMedia existing = mediaMapper.selectOne(
                new LambdaQueryWrapper<FamilyMedia>()
                        .eq(FamilyMedia::getFamilyId, familyId)
                        .eq(FamilyMedia::getPhotoId, photoId)
        );
        if (existing == null) {
            FamilyMedia media = new FamilyMedia();
            media.setFamilyId(familyId);
            media.setPhotoId(photoId);
            media.setSharedBy(userId);
            mediaMapper.insert(media);
        }
    }

    public void unshareMedia(Long familyId, Long photoId) {
        mediaMapper.delete(
                new LambdaQueryWrapper<FamilyMedia>()
                        .eq(FamilyMedia::getFamilyId, familyId)
                        .eq(FamilyMedia::getPhotoId, photoId)
        );
    }

    public void unshareFromAllFamilies(Long userId, Long photoId) {
        // 查找用户拥有的家庭中共享了这张照片的记录
        List<Family> ownedFamilies = getOwnedFamilies(userId);
        for (Family family : ownedFamilies) {
            mediaMapper.delete(
                    new LambdaQueryWrapper<FamilyMedia>()
                            .eq(FamilyMedia::getFamilyId, family.getId())
                            .eq(FamilyMedia::getPhotoId, photoId)
                            .eq(FamilyMedia::getSharedBy, userId)
            );
        }
        // 也从用户是成员的家庭中移除自己共享的记录
        List<FamilyMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getUserId, userId)
                        .eq(FamilyMember::getStatus, 1)
        );
        for (FamilyMember m : memberships) {
            mediaMapper.delete(
                    new LambdaQueryWrapper<FamilyMedia>()
                            .eq(FamilyMedia::getFamilyId, m.getFamilyId())
                            .eq(FamilyMedia::getPhotoId, photoId)
                            .eq(FamilyMedia::getSharedBy, userId)
            );
        }
    }

    public List<Photo> getFamilyMedia(Long familyId) {
        List<FamilyMedia> mediaList = mediaMapper.selectList(
                new LambdaQueryWrapper<FamilyMedia>()
                        .eq(FamilyMedia::getFamilyId, familyId)
                        .orderByDesc(FamilyMedia::getShareTime)
        );
        List<Photo> photos = new ArrayList<>();
        for (FamilyMedia m : mediaList) {
            Photo p = photoMapper.selectById(m.getPhotoId());
            if (p != null && p.getIsDeleted() == 0) photos.add(p);
        }
        return photos;
    }
}
