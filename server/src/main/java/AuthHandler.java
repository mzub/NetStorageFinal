import io.netty.buffer.ByteBuf;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Handles a server-side channel.
 */
public class AuthHandler extends ChannelInboundHandlerAdapter { // (1)

    boolean isAuthOK = false;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) { // (2)
        System.out.println("AuthHandler got data");
        ByteBuf in = (ByteBuf) msg;
        if (isAuthOK) {
            ctx.fireChannelRead(msg);
        } else if (in.readByte() == 12) {
            int loginLength = in.readInt();
            byte[] loginBytes = new byte[loginLength];
            in.readBytes(loginBytes);
            String login = new String(loginBytes);
            int passwLength = in.readInt();
            byte[] passwBytes = new byte[passwLength];
            in.readBytes(passwBytes);
            String passw = new String(passwBytes);
            System.out.println(login);
            System.out.println(passw);

            DataBaseConnector dataBaseConnector = new DataBaseConnector();
            ArrayList<String[]> usersData = dataBaseConnector.getLogins();
            usersData.stream().map(Arrays::toString).forEach(System.out::println);
            for (String[] userData: usersData
                 ) {
                if (userData[0].equals(login) && userData[1].equals(passw)) {
                    System.out.println("Auth is OK");
                    isAuthOK = true;
                    ctx.writeAndFlush(Unpooled.buffer(1).writeByte(12));
                    ctx.fireUserEventTriggered("auth_ok");
                    break;
                }
            }
            if (!isAuthOK) {
                System.err.println("Auth is WRONG");
                ctx.writeAndFlush(Unpooled.buffer(1).writeByte(5));
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) { // (4)
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }
}