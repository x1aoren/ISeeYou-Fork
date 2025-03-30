package cn.xor7.iseeyou.commands;

import cn.xor7.iseeyou.ISeeYouClient;
import cn.xor7.iseeyou.recording.ReplayManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentument;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * 摄像机命令处理类
 * 实现/photographer命令，允许创建和管理摄像机
 */
public class PhotographerCommand {
    private static final Map<String, ServerPlayerEntity> activeCameras = new HashMap<>();

    /**
     * 注册命令
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("photographer")
                .requires(source -> source.hasPermissionLevel(2)) // 需要OP权限
                
                // /photographer create <name> [location]
                .then(CommandManager.literal("create")
                    .then(CommandManager.argument("name", StringArgumentument.word())
                        .executes(PhotographerCommand::createCamera)
                        .then(CommandManager.argument("location", Vec3ArgumentType.vec3())
                            .executes(PhotographerCommand::createCameraAtLocation)
                        )
                    )
                )
                
                // /photographer remove <name>
                .then(CommandManager.literal("remove")
                    .then(CommandManager.argument("name", StringArgumentument.word())
                        .executes(PhotographerCommand::removeCamera)
                    )
                )
                
                // /photographer list
                .then(CommandManager.literal("list")
                    .executes(PhotographerCommand::listCameras)
                )
        );
    }

    /**
     * 在玩家当前位置创建摄像机
     */
    private static int createCamera(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrThrow();
        String name = StringArgumentument.getString(context, "name");
        
        // 检查名称长度
        if (name.length() < 5 || name.length() > 16) {
            source.sendError(Text.literal("相机名称必须在5到16个字符之间"));
            return 0;
        }
        
        // 检查名称是否已存在
        if (activeCameras.containsKey(name)) {
            source.sendError(Text.literal("已存在同名相机: " + name));
            return 0;
        }
        
        // 添加摄像机
        activeCameras.put(name, player);
        boolean success = ReplayManager.startCustomRecording(player, name);
        
        if (success) {
            source.sendFeedback(() -> Text.literal("已创建摄像机: " + name), true);
            return 1;
        } else {
            source.sendError(Text.literal("创建摄像机失败"));
            activeCameras.remove(name);
            return 0;
        }
    }

    /**
     * 在指定位置创建摄像机
     */
    private static int createCameraAtLocation(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        String name = StringArgumentument.getString(context, "name");
        Vec3d location = Vec3ArgumentType.getVec3(context, "location");
        
        // 检查名称长度
        if (name.length() < 5 || name.length() > 16) {
            source.sendError(Text.literal("相机名称必须在5到16个字符之间"));
            return 0;
        }
        
        // 检查名称是否已存在
        if (activeCameras.containsKey(name)) {
            source.sendError(Text.literal("已存在同名相机: " + name));
            return 0;
        }
        
        // 尝试获取玩家
        ServerPlayerEntity player = null;
        try {
            player = source.getPlayerOrThrow();
        } catch (CommandSyntaxException e) {
            // 命令是从控制台执行的，这是允许的
            // 我们会使用server上的任何一个玩家作为摄像机的基础
            if (ISeeYouClient.getServer().getPlayerManager().getPlayerList().isEmpty()) {
                source.sendError(Text.literal("无法在没有玩家在线的情况下创建摄像机"));
                return 0;
            }
            player = ISeeYouClient.getServer().getPlayerManager().getPlayerList().get(0);
            ISeeYouClient.LOGGER.info("使用玩家 {} 作为摄像机基础", player.getName().getString());
        }
        
        // 添加摄像机
        activeCameras.put(name, player);
        boolean success = ReplayManager.startCustomRecordingAtLocation(player, name, location);
        
        if (success) {
            source.sendFeedback(() -> Text.literal("已在位置 " + String.format("%.2f, %.2f, %.2f", 
                    location.x, location.y, location.z) + " 创建摄像机: " + name), true);
            return 1;
        } else {
            source.sendError(Text.literal("创建摄像机失败"));
            activeCameras.remove(name);
            return 0;
        }
    }

    /**
     * 移除摄像机
     */
    private static int removeCamera(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String name = StringArgumentument.getString(context, "name");
        
        // 检查摄像机是否存在
        if (!activeCameras.containsKey(name)) {
            source.sendError(Text.literal("找不到摄像机: " + name));
            return 0;
        }
        
        // 移除摄像机
        ServerPlayerEntity player = activeCameras.get(name);
        activeCameras.remove(name);
        boolean success = ReplayManager.stopCustomRecording(player, name);
        
        if (success) {
            source.sendFeedback(() -> Text.literal("已移除摄像机: " + name), true);
            return 1;
        } else {
            source.sendError(Text.literal("移除摄像机失败"));
            return 0;
        }
    }

    /**
     * 列出所有摄像机
     */
    private static int listCameras(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        
        if (activeCameras.isEmpty()) {
            source.sendFeedback(() -> Text.literal("没有活动的摄像机"), false);
            return 0;
        }
        
        StringBuilder message = new StringBuilder("活动的摄像机: \n");
        for (String name : activeCameras.keySet()) {
            message.append("- ").append(name).append("\n");
        }
        
        source.sendFeedback(() -> Text.literal(message.toString()), false);
        return activeCameras.size();
    }
} 