package com.fangdd.tp.dao;

import com.fangdd.tp.entity.User;
import org.springframework.stereotype.Repository;

/**
 * @author xuwenzhen
 * @date 18/1/23
 */
@Repository
public class UserDao extends BaseDuoDocEntityDao<User> {
    @Override
    protected String getCollectionName() {
        return "user";
    }
}
