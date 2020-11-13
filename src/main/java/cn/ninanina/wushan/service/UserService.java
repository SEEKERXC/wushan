package cn.ninanina.wushan.service;

import cn.ninanina.wushan.domain.User;

public interface UserService {
    /**
     * 用户注册，生成新的用户对象并返回
     * <p>UUID是一个长度为8的字符串，64bit，根据long型整数生成。
     * @return 用户对象，包含id、UUID、nickname等
     */
    User register(String password);

    /**
     * 用户登录，根据UUID和password
     * @return 如果不为空，则登录成功，否则登录失败
     */
    User login(int UUID, String password);

}
