package cn.ninanina.wushan.service;

import cn.ninanina.wushan.common.Gender;
import cn.ninanina.wushan.domain.User;

public interface UserService {
    /**
     * 用户注册，生成新的用户对象并返回
     *
     * @return 用户对象，包含id、username、nickname等
     */
    User register(String appKey, String username, String password, String nickname, Gender gender);

    /**
     * 用户登录
     *
     * @return 如果不为空，则登录成功，否则登录失败
     */
    User login(String username, String password);

    User update(long userId, String password, String nickname, Gender gender, int age, boolean straight);

}
