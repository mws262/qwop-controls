package flashqwop;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.*;


public class SimpleServer
{
    public static void main(String args[])
    {
        // Message terminator
        char EOF = (char)0x00;

        try
        {
            // create a serverSocket connection on port
            ServerSocket s = new ServerSocket(2900);

            System.out.println("Server started. Waiting for connections...");
            // wait for incoming connections
            Socket incoming = s.accept();

            BufferedReader data_in = new BufferedReader(
                    new InputStreamReader(incoming.getInputStream()));
//            incoming.setContentType("text/html; charset=UTF-8");

            PrintWriter data_out = new PrintWriter(incoming.getOutputStream());


            boolean quit = false;

            Robot robot = new Robot();
//            robot.setAutoDelay(1);
            // Waits for the EXIT command
            while (!quit)
            {
//                Thread.sleep(1000);

//                robot.keyPress(KeyEvent.VK_Q);
//                long t1 = System.nanoTime();
//
//                while (!data_in.ready()) {}
//                System.out.println("AtReady: " + (System.nanoTime() - t1)/1e6f);
//


                while (data_in.ready()
                ) {
                    char msg = (char)data_in.read();
                    System.out.println(msg);
                }
//                System.out.println("AfterRead: " + (System.nanoTime() - t1)/1e6f);
//                robot.keyRelease(KeyEvent.VK_Q);

                String ms = "10010\0";
                data_out.print(ms);
                data_out.flush();
                Thread.sleep(1000);
                ms = "01100\0";
                data_out.print(ms);
                data_out.flush();
                Thread.sleep(1000);
                ms = "00001\0";
                data_out.print(ms);
                data_out.flush();
                Thread.sleep(1000);


//                data_out.println("<hi>0</hi>");
//                data_out.flush();


//                Thread.sleep(1000);
//                data_out.println("hi");
//                data_out.flush();r
//                if (msg == null) break;

//                if (msg.contains("<policy-file-request/>")) {
//                    System.out.println("asked for the policy file.");
//                    while (data_in.ready()){
//                        data_in.readLine();
//                        System.out.println("read additional line.");
//                    }
//
////                    msg = data_in.readLine();
////                    System.out.println(msg + "hi");
//                    data_out.println("<?xml version='1.0'?>" +
//                            "<cross-domain-policy>" +
//                            "<site-control permitted-cross-domain-policies='all'/>" +
//                            "<allow-access-from domain='*' to-ports='*' secure='false'/>" +
//                            "</cross-domain-policy>\0");
//                    data_out.flush();
//                    System.out.println("sent?");
//                }
            }
        }
        catch (Exception e)
        {
            System.out.println("Connection lost");
        }
    }
}