package yzh.nas.business.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import yzh.nas.business.entity.City;
import yzh.nas.business.mapper.CityMapper;

import java.util.List;

@Service
public class CityService {

    private final CityMapper cityMapper;
    private final JdbcTemplate jdbcTemplate;

    public CityService(CityMapper cityMapper, JdbcTemplate jdbcTemplate) {
        this.cityMapper = cityMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<City> listAll() {
        return cityMapper.selectList(
                new LambdaQueryWrapper<City>().orderByAsc(City::getName)
        );
    }

    public City getById(Long id) {
        return cityMapper.selectById(id);
    }

    public void createCity(City city) {
        cityMapper.insert(city);
    }

    public void updateCity(Long id, City city) {
        city.setId(id);
        cityMapper.updateById(city);
    }

    public void deleteCity(Long id) {
        cityMapper.deleteById(id);
    }

    public long countPhotosByName(String name) {
        List<java.util.Map<String, Object>> result = jdbcTemplate.queryForList(
                "SELECT COUNT(*) as cnt FROM photo WHERE city = ?", name
        );
        if (result.isEmpty()) return 0;
        Object cnt = result.get(0).get("cnt");
        return cnt instanceof Number ? ((Number) cnt).longValue() : 0;
    }
}
