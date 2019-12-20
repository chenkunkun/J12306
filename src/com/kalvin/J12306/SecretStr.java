package com.kalvin.J12306;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpException;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONException;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import com.kalvin.J12306.config.Constants;
import com.kalvin.J12306.dto.TicketInfoDTO;
import com.kalvin.J12306.utils.J12306Util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class SecretStr {
    private static final Log log = LogFactory.get();
    //主要是刷secretStr
    volatile static List<TicketInfoDTO> ticketInfo = new ArrayList<>();

    //获取票的数量
    private static Runnable getThread(final int i) {
        return new Runnable() {
            @Override
            public void run() {
                String date = "2019-12-31";
                String fromStation = "HZH";
                String toStation = "GXN";
                String trainNum = "K656";
                while (true) {
                    getData(date, fromStation, toStation, trainNum);
                }
            }
        };
    }

    /**
     * 获取票的信息
     *
     * @param date        日期 2020-01-18
     * @param fromStation 始发站 HZH
     * @param toStation   到站 GXN
     * @param trainNum    车次 K656
     */
    private static void getData(String date, String fromStation, String toStation, String trainNum) {
        String url = "https://kyfw.12306.cn/otn/leftTicket/queryZ?leftTicketDTO.train_date=" + date + "&leftTicketDTO.from_station=" + fromStation + "&leftTicketDTO.to_station=" + toStation + "&purpose_codes=ADULT";
        HttpRequest getRequest = HttpUtil.createGet(url);
        getRequest.header("Host", Constants.HOST);
        getRequest.header("Connection", "keep-alive");
        getRequest.header("Cache-Control", "no-cache");
        getRequest.header("Accept", "*/*");
        getRequest.header("Referer", "https://kyfw.12306.cn/otn/leftTicket/init?linktypeid=dc&fs=%E6%9D%AD%E5%B7%9E,HZH&ts=%E5%9B%BA%E5%A7%8B,GXN&date=" + date + "&flag=N,N,Y");
        getRequest.header("Cookie", "JSESSIONID=DF9D1A1457752B401280F4277D447D62; route=6f50b51faa11b987e576cdb301e545c4; BIGipServerotn=502268426.64545.0000; _jc_save_fromStation=%u676D%u5DDE%2CHZH; _jc_save_toStation=%u56FA%u59CB%2CGXN; _jc_save_fromDate=2020-01-18; _jc_save_toDate=2019-12-20; _jc_save_wfdc_flag=dc; RAIL_EXPIRATION=1577175388543; RAIL_DEVICEID=kxyIgL3vn4miPNGy9OKlzd_wG6lzwUOXoDbpmuK1Tnj9m6rZk9INHeNr6uBdW9NT339Dw9J4z4QyvIYA_16V2riK_ilRGrqgVui7IxMNygtzBdrpig4Fr_3V7mNJJKNhTwhPw1Qbo7YZC6PMMFHH_BM6FyNDRO62");
        getRequest.header("Sec-Fetch-Mode", "cors");
        getRequest.header("Sec-Fetch-Site", "same-origin");
        getRequest.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.70 Safari/537.36");
        getRequest.header("X-Requested-With", "XMLHttpRequest");
        getRequest.header("If-Modified-Since", "0");
        getRequest.header("Accept-Encoding", "gzip, deflate, br");
        getRequest.header("Accept-Language", "zh-CN,zh;q=0.9");

        try {
            HttpResponse httpResponse = getRequest.execute();
            String body = httpResponse.body();
            try {
                List<TicketInfoDTO> ticketInfoDTOS = J12306Util.parseTicketInfo(body);
                ticketInfoDTOS = ticketInfoDTOS.stream().filter(item -> trainNum.equals(item.getTrainNum())).collect(Collectors.toList());
                for (TicketInfoDTO ticketInfoDTO : ticketInfoDTOS) {
                    String hardBerth = ticketInfoDTO.getL2HardBerth();

                    boolean hardBerthBool = (NumberUtil.isNumber(hardBerth) && Integer.parseInt(hardBerth) > 3) || "有".equals(hardBerth);

                    if (hardBerthBool) {
                        ticketInfo.add(ticketInfoDTO);
                        log.info("获取一个secretStr，{}", ticketInfoDTO);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (HttpException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        //线程池获取
        ExecutorService fixPool = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 100; i++) {
            fixPool.execute(getThread(i));
        }
//        fixPool.shutdown();
    }
}
