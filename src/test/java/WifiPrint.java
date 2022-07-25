import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class WifiPrint {


    public static void main(String[] args) {

        try {
            Socket sock = new Socket("10.13.0.250", 9100); // ip and port of printer
            OutputStream outputStream = sock.getOutputStream();

            File file = new File("C:\\Users\\Administrator\\Desktop\\1.docx");
            try(FileInputStream fis = new FileInputStream(file)){
                byte[] bytes = new byte[1024];
                int len;
                while (true){
                    len = fis.read(bytes);
                    System.out.println("读取字节: "+ len);
                    if (len == -1) break;
                    outputStream.write(bytes,0,len);
                }
            }
            outputStream.write("\n\n\n".getBytes());
            outputStream.close();
            sock.close();
            System.out.println("*****");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
