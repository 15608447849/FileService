package server.comm;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.StringTokenizer;

import static server.comm.RuntimeUtil.byteLength2StringShow;

public class NetWorkUtil {

    private static long[] readInLine(BufferedReader input, String osType) {
        long arr[] = new long[2];
        StringTokenizer tokenStat = null;
        try {
            if (osType.equals("linux")) { // 获取linux环境下的网口上下行速率
                long rx = 0, tx = 0;
                String line ;
                //RX packets:4171603 errors:0 dropped:0 overruns:0 frame:0
                //TX packets:4171603 errors:0 dropped:0 overruns:0 carrier:0
                while ((line = input.readLine()) != null) {
                    if (line.contains("RX packets")) {
                        rx += Long.parseLong(line.substring(line.indexOf("RX packets") + 11, line.indexOf(" ", line.indexOf("RX packets") + 11)));
                    } else if (line.contains("TX packets")) {
                        tx += Long.parseLong(line.substring(line.indexOf("TX packets") + 11, line.indexOf(" ", line.indexOf("TX packets") + 11)));
                    }
                }
                arr[0] = rx;
                arr[1] = tx;
            } else { // 获取windows环境下的网口上下行速率
                input.readLine();
                input.readLine();
                input.readLine();
                input.readLine();
                tokenStat = new StringTokenizer(input.readLine());
                tokenStat.nextToken();
                arr[0] = Long.parseLong(tokenStat.nextToken());
                arr[1] = Long.parseLong(tokenStat.nextToken());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return arr;
    }

    //    private static String formatNumber(double f) {
//
//        return new Formatter().format("%.2f", f).toString();
//    }
    private static String formatNumber(long f) {
        System.out.println(f);
        return byteLength2StringShow(f);
    }
    //获取网络上行下行速度
    private static Map<String, String> getNetworkDownUp() {
        Properties props = System.getProperties();
        String os = props.getProperty("os.name").toLowerCase();
        os = os.startsWith("win") ? "windows" : "linux";
        Map<String, String> result = new HashMap<>();

        String rxPercent = "";
        String txPercent = "";
        try {
            String command = "windows".equals(os) ? "netstat -e" : "ifconfig";
            Runtime r = Runtime.getRuntime();
            Process pro = r.exec(command);
            BufferedReader input = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            long[] result1 = readInLine(input, os);
            pro.destroy();
            input.close();

            Thread.sleep(1000);

            pro = r.exec(command);
            input = new BufferedReader(new InputStreamReader(pro.getInputStream()));
            long[] result2 = readInLine(input, os);
            pro.destroy();
            input.close();

            pro.destroy();

            rxPercent = formatNumber((result2[0] - result1[0])  ); // 下行速率
            txPercent = formatNumber((result2[1] - result1[1]) ); // 上行速率

        } catch (Exception e) {
            e.printStackTrace();
        }

        result.put("rxPercent", rxPercent);// 下行速率
        result.put("txPercent", txPercent);// 上行速率
        return result;

    }



    public static void main(String[] args) {

        while (true){
            Map<String, String> result = getNetworkDownUp();
            System.out.println("下行速率:"+ result.get("rxPercent")+" 上行速率:"+ result.get("txPercent"));
        }
    }

}
