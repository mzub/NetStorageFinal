import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import static io.netty.buffer.Unpooled.*;
import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


public class NetStorageClient {

    String host = "localhost";
    int port = 9779;
    EventLoopGroup workerGroup = new NioEventLoopGroup();
    ChannelFuture f;
    ClientGUI gui;

    public NetStorageClient(ClientGUI gui) {
        this.gui = gui;
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast( new ClientHandler(gui), new ObjectDecoder( ClassResolvers.cacheDisabled(null)));
                }
            });
            b.option(ChannelOption.SO_KEEPALIVE, true);
            // Start the client.
            f = b.connect(host, port).sync();
            gui.getButtonCopy().addActionListener(event -> {
                try {
                    if (!gui.getUserPane().isSelectionEmpty()) {
                        sendFile(Paths.get("client/repository/" + gui.getUserPane().getSelectedValue()));
                    } else if (!gui.getServerPane().isSelectionEmpty()) {
                        getFile((String) gui.getServerPane().getSelectedValue());
                    } else {
                        JOptionPane.showMessageDialog(gui.getFrame(), "Выберите файл для копирования");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            gui.getButtonDelete().addActionListener(event -> {
                if (!gui.getUserPane().isSelectionEmpty()) {
                    try {
                        Files.delete(Paths.get("client/repository/" + gui.getUserPane().getSelectedValue()));
                        gui.getUserPane().setListData(Files.list(Paths.get("client/repository/")).map((Path path) -> path.getFileName().toString()).sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (!gui.getServerPane().isSelectionEmpty()) {
                    deleteFileFromServer((String) gui.getServerPane().getSelectedValue());
                } else {
                    JOptionPane.showMessageDialog(gui.getFrame(), "Выберите файл для удаления");
                }
            });
            gui.getButtonMove().addActionListener(event -> {
                try {
                    if (!gui.getUserPane().isSelectionEmpty()) {
                        Path selectedFile = Paths.get("client/repository/" + gui.getUserPane().getSelectedValue());
                        sendFile(selectedFile);
                        Files.delete(selectedFile);
                        gui.getUserPane().setListData(Files.list(Paths.get("client/repository/")).map((Path path) -> path.getFileName().toString()).sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new));
                    } else if (!gui.getServerPane().isSelectionEmpty()) {
                        String selectedFileName = (String) gui.getServerPane().getSelectedValue();
                        getFile(selectedFileName);
                        deleteFileFromServer(selectedFileName);

                    } else {
                        JOptionPane.showMessageDialog(gui.getFrame(), "Выберите файл для копирования");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
            gui.getAuthWindow().getButtonOk().addActionListener(event -> {
                String login = gui.getAuthWindow().getLogin();
                char[] password = gui.getAuthWindow().getPassword();
                authentication(login, password);
                gui.getAuthWindow().dispose();
            });
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } catch (InterruptedException e ) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    private void sendFile(Path file) throws IOException {
        System.out.println("Клиент отправляет файл: " + file.getFileName().toString());
        Channel ch = f.channel();
        ch.writeAndFlush(buffer().writeByte(15) // сигнальный байт
                                 .writeInt(file.getFileName().toString().length())  // длина имени файла
                                 .writeBytes(file.getFileName().toString().getBytes()) // имя файла
                                 .writeLong(Files.size(file))// размер файла);
                                 .writeBytes(Files.readAllBytes(file))); // сам файл);
        System.out.println("File sent: " + file.getFileName().toString());

    }

    private void getFile(String fileName)  {
        System.out.println("Клиент запросил загрузку файла" + fileName);
        Channel ch = f.channel();
        ch.writeAndFlush(buffer().writeByte(13) // сигнальный байт
                                 .writeInt(fileName.getBytes().length) // длина имени файла);
                                 .writeBytes(fileName.getBytes())); // имя файла);
    }

    private void getListOfFiles() {
        Channel ch = f.channel();
        ch.writeAndFlush(buffer(1).writeByte(11)); // сигнальный байт
    }

    private void deleteFileFromServer(String fileName) {
        System.out.println("Клиент просит удалить файл " + fileName);
        Channel ch = f.channel();
        ch.writeAndFlush(buffer().writeByte(14) // сигнальный байт
                .writeInt(fileName.length()) // длина имени файла);
                .writeBytes(fileName.getBytes())); // имя файла);
    }

    private void authentication(String login, char[] password) {
        System.out.println("Клиент желает авторизоваться, логин " + login + ", пароль " + Arrays.toString(password));
        Channel ch = f.channel();
        ch.writeAndFlush(buffer(1).writeByte(12));
        ch.writeAndFlush(buffer().writeInt(login.getBytes().length) // длина логина);
                                 .writeBytes(login.getBytes()) //байты логина
                                 .writeInt(password.length)//длина пароля
                                 .writeBytes(new String(password).getBytes()));
    }

}