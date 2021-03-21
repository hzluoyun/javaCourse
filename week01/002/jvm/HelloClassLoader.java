package jvm;

import java.io.*;
import java.lang.reflect.Method;
/**
 * @author luojun
 * @description 自定义classloader加载class进行操作
 */
public class HelloClassLoader extends ClassLoader {
    public static void main(String[] args) throws Exception {
        //获取初始化对象
        Class helloClass =  new HelloClassLoader().findClass("Hello");
        Object helloObj = helloClass.newInstance();
        Method method = helloClass.getDeclaredMethod("hello");
        method.setAccessible(true);
        method.invoke(helloObj);

    }
    @Override
    protected Class<?> findClass(String name){
        String userDir = System.getProperty("user.dir");
        File helloFile = new File(userDir + "/week01/002/jvm/Hello.xlass");
        byte[] bytes = loadByteFromFile(helloFile);
        return defineClass(name,decode(bytes),0,bytes.length);
    }
    public static byte[] decode(byte[] bytes){
        for(int i = 0;i < bytes.length;i++){
            bytes[i] = (byte) - (bytes[i] - 255);
        }
        return bytes;
    }

    /**
     * @description 从文件中读取字节流
     */
    public static byte[] loadByteFromFile(File file){
        byte[] buffer = null;
        try{
            if (!file.exists())
            {
                return null;
            }

            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int len=-1;
            while ((len = fis.read(b)) != -1)
            {
                bos.write(b, 0, len);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return buffer;
    }




}

