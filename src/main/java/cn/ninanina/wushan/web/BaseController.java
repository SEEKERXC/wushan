package cn.ninanina.wushan.web;

import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.web.cache.UserCacheManager;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Controller
public abstract class BaseController {
    @Autowired
    private UserCacheManager userCacheManager;

    HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    protected Response result() {
        return Response.success(null);
    }

    protected Response result(ResultMsg message) {
        return Response.message(message);
    }

    protected Response result(Object data) {
        return Response.success(data);
    }

    protected Response result(ResultMsg message, Object data) {
        return Response.messageWithData(message, data);
    }

    protected String getIp() {
        String value = getRequest().getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(value) && !"unknown".equalsIgnoreCase(value)) {
            return value;
        } else {
            return getRequest().getRemoteAddr();
        }
    }

    protected Long getUserId(String token) {
        if (!StringUtils.isEmpty(token))
            return userCacheManager.get(token);
        else return null;
    }

}
