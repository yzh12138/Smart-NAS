package yzh.stock.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import yzh.stock.business.entity.Book;

@Mapper
public interface BookMapper extends BaseMapper<Book> {
}
