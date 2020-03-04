import io.netty.buffer.ByteBuf;

import io.netty.channel.*;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedFile;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static io.netty.buffer.Unpooled.*;

/**
 * Handles a server-side channel.
 */
public class InteractionHandler extends ChannelInboundHandlerAdapter {

    private ServerState serverState = ServerState.WAITING_COMMAND;
    private String fileName;
    private long incFileSize;
    private File newFile;
    private int inputBytesAmount;
    private long bytesOfFile;

    private enum ServerState {
        WAITING_COMMAND, WAITING_A_HEADER, WAITING_A_FILE, DOWNLOAD_A_FILE, GET_LIST_OF_FILES, DELETE;
    }

    private enum DataType {
        EMPTY((byte)-1), GET_FILES_LIST((byte)11), UPLOAD_FILE((byte)15), DOWNLOAD_FILE((byte)13), DELETE_A_FILE((byte)14);
        byte firstMessageByte;

        DataType(byte firstMessageByte) {
            this.firstMessageByte = firstMessageByte;
        }

        static DataType getDataTypeFromByte(byte b) {
            if (b == UPLOAD_FILE.firstMessageByte) {
                return UPLOAD_FILE;
            }
            if (b == DOWNLOAD_FILE.firstMessageByte) {
                return DOWNLOAD_FILE;
            }
            if (b == GET_FILES_LIST.firstMessageByte) {
                return GET_FILES_LIST;
            }
            if (b == DELETE_A_FILE.firstMessageByte){
                return DELETE_A_FILE;
            }
            return EMPTY;
        }
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client connected");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        String eventName = (String) evt;
        if (eventName.equals("auth_ok")) {
            sendFileList(ctx);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        System.out.println("Interaction handler read data");
        System.out.println(serverState);
        ByteBuf in = (ByteBuf) msg;
        if (serverState == ServerState.WAITING_COMMAND) {
            switch (DataType.getDataTypeFromByte(in.readByte())) {
                case EMPTY:
                    System.out.println("Empty");
                    break;
                case GET_FILES_LIST:
                    System.out.println("Command - List of files");
                    serverState = ServerState.GET_LIST_OF_FILES;
                    break;
                case UPLOAD_FILE:
                    System.out.println("Command - Upload a file");
                    serverState = ServerState.WAITING_A_HEADER;
                    break;
                case DOWNLOAD_FILE:
                    System.out.println("Command - Download a file");
                    serverState = ServerState.DOWNLOAD_A_FILE;
                    break;
                case DELETE_A_FILE:
                    System.out.println("Command - Delete a file");
                    serverState = ServerState.DELETE;
                    break;
            }
        }

        if (serverState == ServerState.WAITING_A_HEADER) {
            System.out.println(in.readableBytes());
            if(in.readableBytes() == 0) return;
            System.out.println("Upload a file");
            in.writerIndex(in.readerIndex() + 4);
            int fileNameLength = in.readInt();
            System.out.println("Длина имени файла:" + fileNameLength);
            byte[] fileNameBytes = new byte[fileNameLength];
            in.writerIndex(in.readerIndex() + fileNameBytes.length);
            in.readBytes(fileNameBytes);
            fileName = new String(fileNameBytes);
            System.out.println(ctx.pipeline().first());
            for (byte b: fileNameBytes
                 ) {
                System.out.print(b);
            }
            System.out.println("");
            System.out.println("Имя файла: " + fileName);
            in.writerIndex(in.readerIndex() + 8);
            incFileSize = in.readLong();
            System.out.println("Размер файла: " + incFileSize);
            newFile = newFile();
            serverState = ServerState.WAITING_A_FILE;
        }

        if (serverState == ServerState.WAITING_A_FILE) {
            System.out.println("Readable bytes: " + in.readableBytes());
            inputBytesAmount += in.readableBytes();
            System.out.println("Incoming traffic: " + inputBytesAmount);
            try (OutputStream fileWriter = new BufferedOutputStream(new FileOutputStream(newFile, true))) {
                in.writerIndex(in.capacity());
                while (in.isReadable() && bytesOfFile < incFileSize) {
                    bytesOfFile++;
                    fileWriter.write(in.readByte());  // и записываем их в файл
                }
            }
            System.out.println("Bytes of file: " + bytesOfFile);
            if (inputBytesAmount < incFileSize && bytesOfFile != incFileSize) return;
            System.out.println("file is written to server");
            bytesOfFile = 0;
            inputBytesAmount = 0;
            sendFileList(ctx);
            serverState = ServerState.WAITING_COMMAND;
        }

        if (serverState == ServerState.DOWNLOAD_A_FILE) {
            System.out.println("Download a file");
            if (in.readableBytes() == 0) return;
            int fileNameLength = in.readInt();
            System.out.println("Длина имени файла:" + fileNameLength);
            byte[] fileNameBytes = new byte[fileNameLength];
            in.writerIndex(in.readerIndex() + fileNameBytes.length);
            in.readBytes(fileNameBytes);
            fileName = new String(fileNameBytes);
            System.out.println("Имя файла: " + fileName);
            File file = new File("server/repository/" + fileName);
            System.out.println("Размер файла для отправки клиенту :" + Files.size(file.toPath()));
            //отправка клиенту информации о файле
            ctx.writeAndFlush(buffer().writeByte(13)
                                      .writeInt(fileName.length()) // длина имени файла
                                      .writeBytes(fileNameBytes) // имя файла
                                      .writeLong(Files.size(file.toPath()))); // отправка размера файла
            ctx.writeAndFlush(buffer().writeBytes(Files.readAllBytes(file.toPath()))); // отправка самого файла

            serverState = ServerState.WAITING_COMMAND;

        }
        // отправка списка файлов
        if (serverState == ServerState.GET_LIST_OF_FILES) {
            sendFileList(ctx);
        }

        // удаление файла
        if (serverState == ServerState.DELETE) {
            System.out.println("Delete a file");
            if (in.readableBytes() == 0) return;
            int fileNameLength = in.readInt();
            System.out.println("Длина имени файла:" + fileNameLength);
            byte[] fileNameBytes = new byte[fileNameLength];
            in.writerIndex(in.readerIndex() + fileNameBytes.length);
            in.readBytes(fileNameBytes);
            fileName = new String(fileNameBytes);
            System.out.println("Имя файла: " + fileName);

            Path fileToDelete = Paths.get("server/repository/" + fileName);
            Files.delete(fileToDelete);
            sendFileList(ctx);
            serverState = ServerState.WAITING_COMMAND;
        }
    }

    private void sendFileList(ChannelHandlerContext ctx) throws IOException {
        ctx.writeAndFlush(ctx.alloc().buffer(1).writeByte(11));
        ctx.pipeline().addFirst(new ObjectEncoder());
        String[] fileNames = Files.list(Paths.get("server/repository/")).map((Path path) -> path.getFileName().toString()).toArray(value -> new String[value]);
        ctx.writeAndFlush(fileNames);
        System.out.println(ctx.pipeline().removeFirst());
        serverState = ServerState.WAITING_COMMAND;

    }

    private File newFile() throws IOException {
        File newFile = new File("server/repository/" + fileName);
        if (newFile.exists()) {
            throw new FileAlreadyExistsException("File already exist on the server");
        }
        return newFile;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}