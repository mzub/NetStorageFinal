import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import javax.swing.*;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class ClientHandler extends ChannelInboundHandlerAdapter {

    private ClientGUI gui;
    private String fileName;
    private long incFileSize;
    private File newFile;
    private int inputBytesAmount;
    private long bytesOfFile;
    ClientState clientState = ClientState.WAITING_COMMAND;
    Byte command;
    ByteBuf in;


    public ClientHandler(ClientGUI gui) {
        this.gui = gui;
    }

    public enum ClientState {
        WAITING_COMMAND, WAITING_FILE_INFO, WAITING_A_FILE, WAITING_AN_OBJECT;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        if (clientState != ClientState.WAITING_AN_OBJECT) in = (ByteBuf) msg;
        System.out.println("Some data came to client");
        if (clientState == ClientState.WAITING_COMMAND) {
            command = in.readByte();
            if(command == 11) {
                System.out.println("Клиентом получена команда на прием объекта (списка файлов)");
                clientState = ClientState.WAITING_AN_OBJECT;
            }
            else if(command == 13) {
                System.out.println("Клиентом получена команда на прием файла с сервера");
                clientState = ClientState.WAITING_FILE_INFO;
            } else if(command == 5) {
                System.out.println("Клиентом получена команда о неверных авторизационных данных");
                JOptionPane.showMessageDialog(gui.getFrame(), "Неверный логин и/или пароль");
            } else if(command == 12) {
                System.out.println("Клиентом получена команда об успешной авторизации");
                JOptionPane.showMessageDialog(gui.getFrame(), "Добро пожаловать на сервер");
            } else {
                return;
            }

        }



        if (clientState == ClientState.WAITING_FILE_INFO) {
            System.out.println("Header readable bytes: " + in.readableBytes());
            System.out.println("Получаем информацию о файле");
            int fileNameLength = in.readInt();
            System.out.println("Длина имени файла:" + fileNameLength);
            byte[] fileNameBytes = new byte[fileNameLength];
            in.writerIndex(in.readerIndex() + fileNameBytes.length);
            in.readBytes(fileNameBytes);
            fileName = new String(fileNameBytes);
            System.out.println("Имя файла: " + fileName + ", байт в имени файла: " + fileNameBytes.length);
            in.writerIndex(in.readerIndex() + 8);
            incFileSize = in.readLong();
            System.out.println("Размер файла: " + incFileSize);
            newFile = newFile();
            clientState = ClientState.WAITING_A_FILE;
        }
        if (clientState == ClientState.WAITING_A_FILE) {
            System.out.println("Readable bytes: " + in.readableBytes());
            inputBytesAmount += in.readableBytes();
            System.out.println("Incoming traffic: " + inputBytesAmount);
            try (OutputStream fileWriter = new BufferedOutputStream(new FileOutputStream(newFile, true));
            ) {
                while (in.isReadable() && bytesOfFile < incFileSize) {
                    bytesOfFile++;
                    fileWriter.write(in.readByte());  // и записываем их в файл
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Bytes of file: " + bytesOfFile);
            if (inputBytesAmount < incFileSize && bytesOfFile != incFileSize) return;
            System.out.println("file is written to client");
            gui.getUserPane().setListData(Files.list(Paths.get("client/repository/")).map((Path path) -> path.getFileName().toString()).sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new));
            inputBytesAmount = 0;
            bytesOfFile = 0;

            clientState = ClientState.WAITING_COMMAND;
        }

        if (clientState == ClientState.WAITING_AN_OBJECT) {
            ctx.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    try {
                        if (msg == null) {
                            return;
                        }
                        System.out.println((msg.getClass()));
                        if (msg instanceof String[]) {
                            System.out.println("Array was received");
                            String[] fileNames = (String[]) msg;
                            Arrays.sort(fileNames);
                            gui.getServerPane().setListData(fileNames);
                        } else {
                            System.out.print("Server received a wrong object!");
                        }
                    } finally {
                        ReferenceCountUtil.release(msg);
                        System.out.println(ctx.pipeline().removeLast());

                        clientState = ClientState.WAITING_COMMAND;
                    }
                }
            });
            ctx.fireChannelRead(msg);
        }
    }

    private File newFile() throws IOException {
        File newFile = new File("client/repository/" + fileName);
        if (newFile.exists()) {
            throw new FileAlreadyExistsException("File already exist on the server");
        }
        return newFile;
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}