package com.nunodrama;

import com.github.unidbg.AndroidEmulator;
import com.github.unidbg.Module;
import com.github.unidbg.linux.android.AndroidEmulatorBuilder;
import com.github.unidbg.linux.android.AndroidResolver;
import com.github.unidbg.linux.android.dvm.AbstractJni;
import com.github.unidbg.linux.android.dvm.DalvikModule;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.memory.Memory;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KeyGeneratorApi extends AbstractJni {
    private static final Logger logger = LoggerFactory.getLogger(KeyGeneratorApi.class);
    private final AndroidEmulator emulator;
    private final VM vm;
    private final Module module;

    public KeyGeneratorApi() {
        logger.info("Initializing Unidbg Android Emulator (ARM64)...");
        emulator = AndroidEmulatorBuilder.for64Bit().setProcessName("com.storymatrix.drama").build();
        Memory memory = emulator.getMemory();
        memory.setLibraryResolver(new AndroidResolver(23));

        vm = emulator.createDalvikVM();
        vm.setVerbose(false);
        vm.setJni(this);

        File libFile = new File("src/main/resources/android/arm64-v8a/libsaasCorePlayer.so");
        if (!libFile.exists()) {
            throw new RuntimeException("Cannot find libsaasCorePlayer.so at " + libFile.getAbsolutePath());
        }

        DalvikModule dalvikModule = vm.loadLibrary(libFile, true);
        module = dalvikModule.getModule();
        vm.callJNI_OnLoad(emulator, module);
        
        logger.info("libsaasCorePlayer.so successfully loaded into emulator memory!");
    }

    public String generateKeyForUrl(String url) {
        // TODO: Deep Memory C++ Hook into KeyManager::getEncryptFileKey (0x001f976c)
        // For now, this validates that the Unidbg server can receive traffic and run on Render
        return "673417b733338702ed0d39e35eaa9657"; // EP 60 fallback test
    }

    public static void main(String[] args) {
        KeyGeneratorApi generator = new KeyGeneratorApi();

        int port = 8080;
        String envPort = System.getenv("PORT");
        if (envPort != null) {
            port = Integer.parseInt(envPort);
        }

        Javalin app = Javalin.create().start(port);
        logger.info("NunoDrama Unidbg Key Generator started on port " + port);

        app.get("/api/key", ctx -> {
            String url = ctx.queryParam("url");
            if (url == null || url.isEmpty()) {
                ctx.status(400).result("Missing 'url' query parameter");
                return;
            }

            logger.info("Requesting Key for URL: " + url);
            String aesKey = generator.generateKeyForUrl(url);

            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            response.put("key", aesKey);
            response.put("status", "success");

            ctx.json(response);
        });
        
        app.get("/health", ctx -> {
            ctx.result("Unidbg Server is running perfectly.");
        });
    }
}
