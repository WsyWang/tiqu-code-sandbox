import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 运行其他程序
 */
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator + "/src/main/resources/木马程序.bat";
        String errorProgram = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        Process runProcess = Runtime.getRuntime().exec(filePath);
        runProcess.waitFor();
        //分批获取进程的输出
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
        String outputLine;
        while ((outputLine = bufferedReader.readLine()) != null) {
            System.out.println(outputLine);
        }
        System.out.println("执行正常程序成功");
    }
}