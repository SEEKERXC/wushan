package cn.ninanina.wushan.web.result;

import lombok.Data;

@Data
public class Response {
    /**
     * 返回信息码
     */
    private String rspCode = "000000";
    /**
     * 返回信息内容
     */
    private String rspMsg = "操作成功";

    /**
     * 返回数据
     */
    private Object data;

    private Response(){}

    public static Response message(ResultMsg msg) {
        Response response = new Response();
        response.setRspCode(msg.getCode());
        response.setRspMsg(msg.getMsg());
        return response;
    }

    public static Response messageWithData(ResultMsg msg, Object data) {
        Response response = message(msg);
        response.setData(data);
        return response;
    }

    public static Response success(Object data) {
        return messageWithData(ResultMsg.SUCCESS, data);
    }

}
