package com.ashbill.trainresync.test;

import cpw.mods.jarhandling.SecureJar;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.jar.JarFile;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.VersionInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.EnvironmentStateTweaker;
import org.spongepowered.asm.mixin.Mixins;

/** Applies the shipped Mixin configuration to the mapped Create class without starting a game. */
public final class ScheduleMixinHost {
    public static void main(String[] args) throws Exception {
        // Supply the mod list used by the production config plugin in this isolated JVM.
        FMLPaths.loadAbsolutePaths(Path.of("").toAbsolutePath());
        var version = FMLLoader.class.getDeclaredField("versionInfo");
        version.setAccessible(true);
        version.set(null, new VersionInfo(args[2], args[3], "", "net.minecraftforge"));
        var create = new ModFile(SecureJar.from(Path.of(args[1])), null, ModFileParser::modsTomlParser);
        LoadingModList.of(List.of(create), create.getModInfos().stream().map(ModInfo.class::cast).toList(), null);

        List<URL> urls = new ArrayList<>();
        for (String path : System.getProperty("java.class.path").split(File.pathSeparator))
            urls.add(Path.of(path).toUri().toURL());
        try (var jar = new JarFile(args[0])) {
            String manifest = jar.getManifest().getMainAttributes().getValue("MixinConfigs");
            if (manifest == null) throw new AssertionError("Missing MixinConfigs in " + args[0]);
            var configs = List.of(manifest.split("\\s*,\\s*"));
            Launch.classLoader = new LaunchClassLoader(urls.toArray(URL[]::new)) {
                @Override public InputStream getResourceAsStream(String name) {
                    if (!configs.contains(name)) return super.getResourceAsStream(name);
                    var entry = jar.getJarEntry(name);
                    if (entry == null) throw new AssertionError("Missing packaged Mixin config: " + name);
                    try { return jar.getInputStream(entry); }
                    catch (IOException e) { throw new UncheckedIOException(e); }
                }
            };
            Launch.classLoader.addClassLoaderExclusion("net.minecraftforge.");
            Launch.classLoader.addClassLoaderExclusion("com.ashbill.trainresync.mixin.TrainResyncMixinPlugin");
            Launch.blackboard = new HashMap<>();
            Launch.blackboard.put("TweakClasses", new ArrayList<String>());
            Launch.blackboard.put("Tweaks", new ArrayList<>());
            Thread.currentThread().setContextClassLoader(Launch.classLoader);
            MixinBootstrap.init();
            configs.forEach(Mixins::addConfiguration);
            new EnvironmentStateTweaker().getLaunchArguments();

            String target = "com.simibubi.create.content.trains.schedule.ScheduleRuntime";
            byte[] bytes = Launch.classLoader.getClassBytes(target);
            for (var transformer : Launch.classLoader.getTransformers())
                bytes = transformer.transform(target, target, bytes);
            // Define the woven class before loading the test that references it.
            Class<?> samePackage = Class.forName(target + "$State", false, ScheduleMixinHost.class.getClassLoader());
            MethodHandles.privateLookupIn(samePackage, MethodHandles.lookup()).defineClass(bytes);
            ScheduleEntryRegression.run();
        }
    }
}
