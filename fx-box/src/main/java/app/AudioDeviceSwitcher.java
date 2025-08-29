package app;

import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

/**
 * 音频设备切换器 - 真正切换Windows默认音频设备
 */
public class AudioDeviceSwitcher {
    
    /**
     * 切换到指定设备并播放音频
     */
    public static CompletableFuture<MediaPlayer> switchAndPlay(AudioDeviceManager.AudioDevice device, File audioFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 1. 切换到指定设备
                if (switchToDevice(device)) {
                    System.out.println("成功切换到设备: " + device.name);
                    
                    // 2. 等待设备切换完成
                    Thread.sleep(1000);
                    
                    // 3. 在主线程创建MediaPlayer
                    CompletableFuture<MediaPlayer> playerFuture = new CompletableFuture<>();
                    
                    Platform.runLater(() -> {
                        try {
                            MediaPlayer player = new MediaPlayer(new Media(audioFile.toURI().toString()));
                            player.setOnReady(() -> {
                                System.out.println("音频已准备就绪，开始播放到设备: " + device.name);
                                player.play();
                            });
                            player.setOnError(() -> {
                                System.err.println("播放错误: " + player.getError());
                                playerFuture.completeExceptionally(new RuntimeException("播放失败"));
                            });
                            playerFuture.complete(player);
                        } catch (Exception e) {
                            playerFuture.completeExceptionally(e);
                        }
                    });
                    
                    return playerFuture.get();
                } else {
                    System.err.println("设备切换失败，使用当前默认设备");
                    throw new RuntimeException("设备切换失败");
                }
                
            } catch (Exception e) {
                System.err.println("切换设备并播放失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * 切换到指定音频设备
     */
    private static boolean switchToDevice(AudioDeviceManager.AudioDevice device) {
        // 方法1: 使用nircmd (最可靠)
        if (tryNircmd(device)) {
            return true;
        }
        
        // 方法2: 使用PowerShell AudioDeviceCmdlets
        if (tryAudioDeviceCmdlets(device)) {
            return true;
        }
        
        // 方法3: 使用SoundVolumeView (如果可用)
        if (trySoundVolumeView(device)) {
            return true;
        }
        
        System.out.println("所有设备切换方法都失败，将使用当前默认设备");
        return false;
    }
    
    /**
     * 尝试使用nircmd切换设备
     */
    private static boolean tryNircmd(AudioDeviceManager.AudioDevice device) {
        try {
            // 尝试使用nircmd设置默认音频设备
            ProcessBuilder pb = new ProcessBuilder("nircmd.exe", "setdefaultsounddevice", device.name);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("nircmd成功切换到: " + device.name);
                return true;
            }
        } catch (Exception e) {
            // nircmd不可用，尝试下一种方法
        }
        return false;
    }
    
    /**
     * 尝试使用AudioDeviceCmdlets切换设备
     */
    private static boolean tryAudioDeviceCmdlets(AudioDeviceManager.AudioDevice device) {
        try {
            // 使用PowerShell AudioDeviceCmdlets模块
            String command = String.format(
                "Get-AudioDevice -List | Where-Object {$_.Name -like '*%s*'} | Set-AudioDevice",
                device.name.replace("'", "''")
            );
            
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", command);
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("AudioDeviceCmdlets成功切换到: " + device.name);
                return true;
            }
        } catch (Exception e) {
            // AudioDeviceCmdlets不可用
        }
        return false;
    }
    
    /**
     * 尝试使用SoundVolumeView切换设备
     */
    private static boolean trySoundVolumeView(AudioDeviceManager.AudioDevice device) {
        try {
            // 使用SoundVolumeView命令行工具
            ProcessBuilder pb = new ProcessBuilder("SoundVolumeView.exe", "/SetDefault", device.name, "all");
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                System.out.println("SoundVolumeView成功切换到: " + device.name);
                return true;
            }
        } catch (Exception e) {
            // SoundVolumeView不可用
        }
        return false;
    }
    
    /**
     * 测试设备切换
     */
    public static CompletableFuture<Boolean> testDeviceSwitch(AudioDeviceManager.AudioDevice device) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取当前默认设备
                String originalDevice = getCurrentDefaultDevice();
                
                // 切换到目标设备
                boolean switched = switchToDevice(device);
                
                if (switched) {
                    // 播放测试音
                    Thread.sleep(500);
                    playTestSound();
                    
                    // 可选：切换回原设备
                    // switchBackToOriginal(originalDevice);
                    
                    return true;
                }
                
                return false;
                
            } catch (Exception e) {
                System.err.println("测试设备切换失败: " + e.getMessage());
                return false;
            }
        });
    }
    
    /**
     * 获取当前默认设备
     */
    private static String getCurrentDefaultDevice() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-Command",
                "Get-AudioDevice -Playback | Where-Object {$_.Default -eq $true} | Select-Object -ExpandProperty Name"
            );
            
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
            String deviceName = reader.readLine();
            
            if (deviceName != null && !deviceName.trim().isEmpty()) {
                return deviceName.trim();
            }
            
        } catch (Exception e) {
            System.err.println("获取当前默认设备失败: " + e.getMessage());
        }
        
        return "Unknown";
    }
    
    /**
     * 播放测试音
     */
    private static void playTestSound() {
        try {
            // 使用Windows系统提示音
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", "[console]::beep(440,500)");
            pb.start();
        } catch (Exception e) {
            System.err.println("播放测试音失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否支持设备切换
     */
    public static boolean isSupported() {
        // 检查是否在Windows系统
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return false;
        }
        
        // 检查PowerShell是否可用
        try {
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command", "echo test");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 安装推荐的工具提示
     */
    public static String getInstallationTips() {
        StringBuilder tips = new StringBuilder();
        tips.append("为了获得最佳的音频设备切换体验，建议安装以下工具之一：\n\n");
        tips.append("1. NirCmd (推荐)\n");
        tips.append("   下载地址: https://www.nirsoft.net/utils/nircmd.html\n");
        tips.append("   将nircmd.exe放到系统PATH中\n\n");
        tips.append("2. AudioDeviceCmdlets PowerShell模块\n");
        tips.append("   安装命令: Install-Module -Name AudioDeviceCmdlets\n\n");
        tips.append("3. SoundVolumeView\n");
        tips.append("   下载地址: https://www.nirsoft.net/utils/sound_volume_view.html\n");
        
        return tips.toString();
    }
}
