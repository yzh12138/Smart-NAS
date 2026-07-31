package yzh.nas.business.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import yzh.nas.business.entity.City;
import yzh.nas.business.service.CityService;

import java.util.Map;

@RestController
@RequestMapping("/api/city")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping("/list")
    public ResponseEntity<?> listCities() {
        return ResponseEntity.ok(Map.of("code", 200, "data", cityService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCity(@PathVariable Long id) {
        City city = cityService.getById(id);
        if (city == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "城市不存在"));
        }
        return ResponseEntity.ok(Map.of("code", 200, "data", city));
    }

    @PostMapping
    public ResponseEntity<?> createCity(@RequestBody City city) {
        if (city.getName() == null || city.getName().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("code", 400, "message", "城市名称不能为空"));
        }
        cityService.createCity(city);
        return ResponseEntity.ok(Map.of("code", 200, "message", "创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCity(@PathVariable Long id, @RequestBody City city) {
        if (city.getName() == null || city.getName().trim().isEmpty()) {
            return ResponseEntity.ok(Map.of("code", 400, "message", "城市名称不能为空"));
        }
        cityService.updateCity(id, city);
        return ResponseEntity.ok(Map.of("code", 200, "message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCity(@PathVariable Long id) {
        City city = cityService.getById(id);
        if (city == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "城市不存在"));
        }
        long photoCount = cityService.countPhotosByName(city.getName());
        if (photoCount > 0) {
            return ResponseEntity.ok(Map.of("code", 400, "message", "该城市已关联 " + photoCount + " 张照片，无法直接删除。请先移除照片上的此城市信息。"));
        }
        cityService.deleteCity(id);
        return ResponseEntity.ok(Map.of("code", 200, "message", "删除成功"));
    }
}
