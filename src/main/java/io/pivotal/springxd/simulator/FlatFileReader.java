package io.pivotal.springxd.simulator;
import java.io.*;
import java.net.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Created by lei_xu on 6/8/16.
 */

public class FlatFileReader{
    public Namespace getCommandLineArgs(String args[]){
        ArgumentParser parser = ArgumentParsers.newArgumentParser("FlatFileReader").defaultHelp(true);
        parser.description("Read the Sample Taxi CSV file and output line by line to specific HTTP(s) endpoint");
        parser.addArgument("--file").help("Full path to the CSV file").required(true);
        parser.addArgument("--url").help("Full URL to output").required(true);
        parser.addArgument("--mode").help("Which output mode to use: 'lines-per-second' (use with --lines-per-second option), 'interactive' (one line for when user hit enter key), 'measure-delay' (Use with --line-trigger-file AND --output-pipe-file)");
        parser.addArgument("--lines-per-second").type(Integer.class).help("Use with --mode, how many lines sent per seconds to the url");
        parser.addArgument("--listen-port").type(Integer.class).help("Use with --mode=measure-delay, what is the TCP sink output port in SpringXD");
        //parser.addArgument("--line-trigger-file").help("Use with --mode=measure-delay, full path to the file consist trigger line number, this program will measure how long it takes for the output to generate");
        //parser.addArgument("--output-pipe-file").help("Use with --mode=measure-delay, read output from this Linux Pipe file");

        Namespace argNs;
        try {
            argNs = parser.parseArgs(args);
            // System.out.println(argNs);
            System.out.println((String)argNs.get("file"));
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
            return null;
        }
        return argNs;
    }

    public static void main(String args[]) throws Exception{

        FlatFileReader rsf = new FlatFileReader();
        Namespace argNs = rsf.getCommandLineArgs(args);
        String file = argNs.getString("file");


        File localfile = new File(file);

        try(FileInputStream csv_stream = new FileInputStream(localfile)){
            rsf.postLine(csv_stream, argNs);
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }


    }

    private void postLine(FileInputStream csv_stream, Namespace argNs) throws Exception{

        String url = argNs.getString("url");
        String mode = argNs.getString("mode");
        Integer listen_port = argNs.getInt("listen_port");

        //Begin read file:
        String line=null;

        switch(mode){
            case "interactive":
                line = this.getLine(csv_stream);

                while(line!=null){
                    System.out.println("Press [Enter] to post one line to HTTP server");
                    System.in.read();
                    try{
                        Unirest.post(url).body(line).asString();
                    }catch(UnirestException e){
                        e.printStackTrace();
                    }
                    line = this.getLine(csv_stream);
                }
                if(line==null){
                    System.out.println("All lines has been read, exit");
                    System.exit(1);
                }
                break;
            case "measure-delay":


                if(listen_port==null){
                    throw new Exception("Please specific --listen-port parameter");
                }

                ServerSocket ss = new ServerSocket(listen_port);

                ConcurrentHashMap<String,String[]> line_map = new ConcurrentHashMap<String,String[]>(1000);//init size is 1000
                Thread t =new Thread(new ProgramOutputConsumer(listen_port,line_map, ss));

//                Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
//                    public void uncaughtException(Thread t, Throwable e) {
//                        System.out.println("Uncaught exception: " + e);
//                    }
//                };
//
//                t.setUncaughtExceptionHandler(h);

                t.start();

                while((line = this.getLine(csv_stream))!=null){
                    try{
                        String key=line.split(",")[0];
                        String value=line;
                        String start_time=((Long)System.currentTimeMillis()).toString();

                        line_map.put(key,new String[]{value, start_time});
                        System.out.println("Save to map with key: "+line.split(",")[0]);

                        Unirest.post(url).body(line).asString();
                        System.out.println("Post to URL with: " + line);

//                        break;
                    } catch(UnirestException e){
                        e.printStackTrace();
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                }
                t.join(1000);
                t.interrupt();

                System.out.println("All lines has been read, exit");
                System.exit(1);





            default:
                break;
        }
    }

    private Integer lineNumber = 0;
    private String getLine(FileInputStream csv_stream){
        String line="";
        try{
            int b=csv_stream.read();
            while(b!=-1){
                line+=(char)b;
                if((char)b == '\n'){
                    lineNumber++;
                    System.out.println("read line " + lineNumber.toString() + ":" + line);
                    return line;
                }
                b=csv_stream.read();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
}

class ProgramOutputConsumer implements Runnable{
    private ConcurrentHashMap<String, String[]> outputQueue;
    ServerSocket ss = null;
    @Override
    public void run() {

        System.out.println("Thread "+this.getClass().getName()+" Started");
        try{
            while(true){
                Socket socket = ss.accept();
                System.out.println("Socket accepted");
                BufferedReader tcp_input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String input = tcp_input.readLine();
                if(input==""||input==null){
                    continue;
                }

                System.out.println("ProgramOutputConsumer Response is: " + input);
                String key =input.split(",")[0];
                if(key==null||key==""){
                    System.out.println("Key not found");
                    continue;
                }
                String[] queue_object = outputQueue.get(key);


                String value = queue_object[0];
                Long start_millisecond = Long.parseLong(queue_object[1]);

                outputQueue.remove(key);
                System.out.println("Time spend for ID:" + key + " is: " + (System.currentTimeMillis() - start_millisecond) + " milliseconds");

                socket.close();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public ProgramOutputConsumer(int listen_port, ConcurrentHashMap<String, String[]> outputQueue, ServerSocket ss){
        this.outputQueue = outputQueue;
        this.ss = ss;
    }

}
