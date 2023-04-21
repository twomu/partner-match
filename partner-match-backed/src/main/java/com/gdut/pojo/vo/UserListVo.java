package com.gdut.pojo.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdut.pojo.User;
import lombok.Data;

@Data
public class UserListVo{
   private Page<User> page;
   private Integer  totalNum;
   private Integer showPageSize;
}
