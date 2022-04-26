package com.mohistmc.bukkit.pluginfix;

import com.mohistmc.bukkit.pluginfix.fix.DynmapFix;
import com.mohistmc.bukkit.pluginfix.fix.MythicMobFix;
import com.mohistmc.bukkit.pluginfix.fix.WorldEditFix;
import com.mohistmc.configuration.MohistConfig;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class PluginFixManager {

    public static byte[] injectPluginFix(String className, byte[] clazz) {
        if (className.equals("com.sk89q.worldedit.bukkit.WorldEditPlugin") && MohistConfig.instance.enableworldeditfix.getValue()) {
            return WorldEditFix.replaceGetKeyByGetKeyForgeAndGetAsString(clazz);
        }
        if(className.equals("com.sk89q.worldedit.bukkit.BukkitAdapter") && MohistConfig.instance.enableworldeditfix.getValue()) {
            return WorldEditFix.replaceAdaptBlockType(WorldEditFix.replaceGetKeyByGetKeyForgeAndGetAsString(clazz));
        }
        if (className.equals("io.lumine.xikage.mythicmobs.volatilecode.v1_16_R3.VolatileWorldHandler_v1_16_R3")) {
            return MythicMobFix.patchVolatileWorldHandler(clazz);
        }
        if (className.endsWith("PaperLib")) {
            return PluginFixManager.removePaper(clazz);
        }
        if (className.equals("org.dynmap.bukkit.helper.v116_4.BukkitVersionHelperSpigot116_4")) {
            return DynmapFix.replaceBukkitVersionHelperSpigot116_4(clazz);
        }
        if (className.equals("org.dynmap.bukkit.helper.BukkitVersionHelperGeneric")) {
            return DynmapFix.replaceBukkitVersionHelperGeneric(clazz);
        }
        return clazz;
    }

    public static byte[] removePaper(byte[] basicClass) {
        ClassReader classReader = new ClassReader(basicClass);
        ClassNode classNode = new ClassNode();
        ClassWriter classWriter = new ClassWriter(0);
        classReader.accept(classNode, 0);
        for (MethodNode methodNode : classNode.methods) {
            if (methodNode.name.equals("isPaper") && methodNode.desc.equals("()Z")) {
                InsnList toInject = new InsnList();
                toInject.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(PluginFixManager.class), "isPaper", "()Z"));
                toInject.add(new InsnNode(IRETURN));
                methodNode.instructions = toInject;
            }
        }
        classNode.accept(classWriter);
        return classWriter.toByteArray();
    }

    public static boolean isPaper() {
        return false;
    }

}
