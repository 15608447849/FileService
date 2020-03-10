package server.servlet.imps;

import bottle.util.HttpUtil;
import com.google.gson.*;
import server.servlet.beans.result.Result;
import server.servlet.iface.Mservlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @Author: leeping
 * @Date: 2019/7/23 10:07
 */
public class ImageOCR extends Mservlet implements HttpUtil.Callback {
    private String imageText;
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doPost(req, resp);
        Result result = new Result();
        String base64 = req.getHeader("image-base64");
        if (base64 != null) {
            HttpUtil.Request request = new HttpUtil.Request(HttpUtil.Request.POST);
                request.setCallback(this);
                request.setUrl("http://ocrwiz.com/upload_image_by_paste");
                request.setFileFormSubmit();
                request.addFormItem(new HttpUtil.FormItem("image", base64));
                request.addFormItem(new HttpUtil.FormItem("submission-type"," paste"));
                request.execute();
                result.value(Result.RESULT_CODE.SUCCESS,imageText);
        };
        writeJson(resp,result);
    }

    @Override
    public void onProgress(File file, long l, long l1) {

    }

    @Override
    public void onResult(HttpUtil.Response response) {
        if (response.isSuccess()){
            try {
                imageText = response.getMessage();
                JsonObject jsonObject = new JsonParser().parse(imageText).getAsJsonObject();
                imageText = jsonObject.get("responseString").getAsString();
                jsonObject = new JsonParser().parse(imageText).getAsJsonObject();
                JsonArray jsonArray = jsonObject.get("words_result").getAsJsonArray();
                StringBuffer sb = new StringBuffer();
                for (JsonElement el : jsonArray){
                    System.out.println(el.getAsJsonObject().get("words").getAsString());
                    sb.append(el.getAsJsonObject().get("words").getAsString());
                }
                imageText = sb.toString();
            } catch (Exception e) {
                e.printStackTrace();
                imageText = "无法识别,请重试";
            }
        }
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }
}
