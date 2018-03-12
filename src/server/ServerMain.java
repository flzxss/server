package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerMain {

    public static void main(String[] args) throws IOException {
        int port = 9003;
        server(port);
    }

    private static void server(int port) throws IOException, UnsupportedEncodingException {
        ServerSocket socketServer = new ServerSocket(port);
        Socket accept = socketServer.accept();
        OutputStream outputStream = accept.getOutputStream();
        outputStream.write("你好".getBytes("UTF-8"));
        outputStream.flush();
        System.out.println("服务端 port=" + port);
        byte[] bys = new byte[1024 * 1024];
        InputStream inputStream = accept.getInputStream();
        int read = -1;
        List<Byte> list = new ArrayList<Byte>();
        while (true) {
            try {
                if ((read = inputStream.read(bys, 0, 1024)) != -1) {
                    for (int i = 0; i < read; i++) {
                        list.add(bys[i]);
                    }
                }
            } catch (SocketException e) {
                analysis(list);
                list = new ArrayList<Byte>();
                e.printStackTrace();
                inputStream.close();
                accept = socketServer.accept();
                outputStream = accept.getOutputStream();
                outputStream.write("开始部署...".getBytes("UTF-8"));
                outputStream.flush();
                inputStream = accept.getInputStream();
            }
        }
    }

    private static String analysis(List<Byte> list) {
        List<Byte> separateList = Collections.nCopies(8, (byte) 5);
        int index = Collections.lastIndexOfSubList(list, separateList);
        if (index != -1) {
            List<Byte> strList = list.subList(0, index);
            List<Byte> fileList = list.subList(index + separateList.size(), list.size());
            byte[] array = convert(strList);
            String metaData = null;
            try {
                metaData = new String(array, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String[] arrayStr = metaData.split(",");
            String filePath = arrayStr[0];
            String projectName = arrayStr[1];
            filePath = filePath.replace("\\", "/").trim();
            if (filePath.endsWith("/")) {
                filePath = filePath.substring(0, filePath.length() - 1);
            }
            String[] fileNames = warFile(filePath, projectName);
            String shutdown = fileNames[1];
            String startup = fileNames[2];
            String r1 = cmd(shutdown);
            del(fileNames[0]);
            writeFile(fileList, fileNames[0]);
            String startupResult = cmd(startup);
            System.out.println("=========startup==========");
            System.out.println(startupResult);
        }
        return null;
    }

    private static void del(String fileName) {
        if (fileName != null && !"".equals(fileName)) {
            File file = new File(fileName);
            if (file.isFile()) {
                file.delete();
            }
            String ext = ext(fileName);
            if (ext != null && fileName.endsWith(ext)) {
                fileName = fileName.substring(0, fileName.length() - ext.length() - 1);
            }
            File folder = new File(fileName);
            if (folder.isDirectory()) {
                delete(folder);
            }
        }

    }

    private static void delete(File folder) {
        if (folder.isDirectory()) {
            File[] listFiles = folder.listFiles();
            if (listFiles.length == 0) {
                folder.delete();
            } else {
                for (File file : listFiles) {
                    delete(file);
                }
                if (folder.listFiles().length == 0) {
                    folder.delete();
                }
            }
        } else {
            folder.delete();
        }

    }

    private static void writeFile(List<Byte> fileList, String fileName) {
        if (fileList != null && fileList.size() != 0) {
            File file = new File(fileName);
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                byte[] fileByte = convert(fileList);
                fileOutputStream.write(fileByte);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String[] warFile(String filePath, String projectName) {

        String simpleFileName = name(filePath);
        String ext = ext(simpleFileName);
        String[] result = new String[3];
        if (ext == null || "".equals(ext)) {
            if (filePath.endsWith("/")) {
                filePath = filePath.substring(0, filePath.length() - 1);
            }
            String shellExt = shellExt();
            String shutdown = filePath + "/bin/shutdown." + shellExt;
            if (new File(shutdown).exists() && new File(filePath + "/webapps").isDirectory()) {
                result[0] = filePath + "/webapps/" + projectName;
                result[1] = shutdown;
                String startup = filePath + "/bin/startup." + shellExt;
                result[2] = startup;
            }
        }
        if (result[0] == null) {
            result[0] = filePath;
        }
        return result;
    }

    private static String shellExt() {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            return "bat";
        } else {
            return "sh";
        }
    }

    private static String name(String filePath) {

        int lastIndexOf = filePath.lastIndexOf("/");
        String name = filePath;
        if (lastIndexOf != -1) {
            name = filePath.substring(lastIndexOf + 1);
        }
        return name;
    }

    private static String ext(String filePath) {
        int lastIndexOf = filePath.lastIndexOf("/");
        String name = filePath;
        if (lastIndexOf != -1) {
            name = filePath.substring(lastIndexOf + 1);
        }
        int pointIndex = name.lastIndexOf(".");
        if (pointIndex != -1) {
            String ext = name.substring(pointIndex + 1);
            try {
                Integer.parseInt(ext);
                return null;
            } catch (NumberFormatException e) {
                return ext;
            }
        }
        return null;
    }

    private static byte[] convert(List<Byte> strList) {
        byte[] bys = new byte[strList.size()];
        for (int i = 0; i < strList.size(); i++) {
            bys[i] = strList.get(i);
        }
        return bys;
    }

    private static String cmd(String cmd) {
        if (cmd == null || "".equals(cmd)) {
            return null;
        }
        Runtime runtime = Runtime.getRuntime();
        String command = cmd;
        String charset = "UTF-8";
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            command = "cmd /c " + cmd;
            charset = "GBK";
        }
        Process process = null;
        try {
            process = runtime.exec(command);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        InputStream inputStream = process.getInputStream();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(inputStream, charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String line = null;
        StringBuilder sb = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
