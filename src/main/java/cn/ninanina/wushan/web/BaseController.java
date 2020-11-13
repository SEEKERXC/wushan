package cn.ninanina.wushan.web;

import cn.ninanina.wushan.common.Constant;
import cn.ninanina.wushan.domain.User;
import cn.ninanina.wushan.web.result.Response;
import cn.ninanina.wushan.web.result.ResultMsg;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public abstract class BaseController {
    HttpServletRequest getRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    }

    /**
     * 一旦获取了session，就设置一个小时过期
     */
    HttpSession getSession() {
        HttpSession session=getRequest().getSession();
        session.setMaxInactiveInterval(3600);
        return getRequest().getSession();
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

    protected User getUser() {
        return (User) getSession().getAttribute(Constant.KEY_USER);
    }

}
