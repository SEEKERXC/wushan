package cn.ninanina.wushan.service.impl;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.repository.UserRepository;
import cn.ninanina.wushan.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Slf4j
public class UserServiceImp implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User register(String password) {
        User user = new User();
        Random random = new Random();
        int randInt = random.nextInt(Constant.USER_SCALE);
        while (userRepository.findByUUID(randInt) != null) {
            randInt = random.nextInt(Constant.USER_SCALE);
        }
        user.setUUID(randInt);
        user.setPassword(password);
        user.setNickname("污友" + randInt);
        user.setRegisterTime(System.currentTimeMillis());
        user.setLastLoginTime(System.currentTimeMillis());
        return user;
    }

    @Override
    public User login(int UUID, String password) {
        User user = userRepository.findByUUIDAndPassword(UUID, password);
        user.setLastLoginTime(System.currentTimeMillis());
        userRepository.save(user);
        return user;
    }

}
