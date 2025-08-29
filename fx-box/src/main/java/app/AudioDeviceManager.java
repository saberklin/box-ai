package app;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 音频设备管理器 - 使用JNA和Java Sound API获取和控制音频设备
 */
public class AudioDeviceManager {
    
    // Windows音频设备信息结构
    public static class AudioDevice {
        public String name;
        public String id;
        public boolean isInput;
        public boolean isDefault;
        
        public AudioDevice(String name, String id, boolean isInput, boolean isDefault) {
            this.name = name;
            this.id = id;
            this.isInput = isInput;
            this.isDefault = isDefault;
        }
        
        @Override
        public String toString() {
            return name + (isDefault ? " (默认)" : "");
        }
    }
    
    /**
     * 获取所有音频输出设备
     */
    public static List<AudioDevice> getOutputDevices() {
        List<AudioDevice> devices = new ArrayList<>();
        
        try {
            // 使用Java Sound API获取音频设备
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            
            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                
                // 检查是否支持音频输出
                Line.Info[] sourceLineInfos = mixer.getSourceLineInfo();
                if (sourceLineInfos.length > 0) {
                    // 检查是否支持SourceDataLine（播放）
                    for (Line.Info lineInfo : sourceLineInfos) {
                        if (lineInfo instanceof DataLine.Info) {
                            DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                            if (SourceDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
                                String deviceName = mixerInfo.getName();
                                String deviceId = mixerInfo.getDescription();
                                boolean isDefault = deviceName.toLowerCase().contains("primary") || 
                                                  deviceName.toLowerCase().contains("default");
                                
                                devices.add(new AudioDevice(deviceName, deviceId, false, isDefault));
                                break;
                            }
                        }
                    }
                }
            }
            
            // 如果没有找到设备，添加默认设备
            if (devices.isEmpty()) {
                devices.add(new AudioDevice("系统默认输出", "default", false, true));
            }
            
        } catch (Exception e) {
            // 添加默认设备作为后备
            devices.add(new AudioDevice("系统默认输出", "default", false, true));
            devices.add(new AudioDevice("扬声器", "speakers", false, false));
            devices.add(new AudioDevice("耳机", "headphones", false, false));
        }
        
        return devices;
    }
    
    /**
     * 获取所有音频输入设备
     */
    public static List<AudioDevice> getInputDevices() {
        List<AudioDevice> devices = new ArrayList<>();
        
        try {
            // 使用Java Sound API获取音频设备
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            
            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                
                // 检查是否支持音频输入
                Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
                if (targetLineInfos.length > 0) {
                    // 检查是否支持TargetDataLine（录音）
                    for (Line.Info lineInfo : targetLineInfos) {
                        if (lineInfo instanceof DataLine.Info) {
                            DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                            if (TargetDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
                                String deviceName = mixerInfo.getName();
                                String deviceId = mixerInfo.getDescription();
                                boolean isDefault = deviceName.toLowerCase().contains("primary") || 
                                                  deviceName.toLowerCase().contains("default");
                                
                                devices.add(new AudioDevice(deviceName, deviceId, true, isDefault));
                                break;
                            }
                        }
                    }
                }
            }
            
            // 如果没有找到设备，添加默认设备
            if (devices.isEmpty()) {
                devices.add(new AudioDevice("系统默认输入", "default", true, true));
            }
            
        } catch (Exception e) {
            // 添加默认设备作为后备
            devices.add(new AudioDevice("系统默认输入", "default", true, true));
            devices.add(new AudioDevice("麦克风", "microphone", true, false));
            devices.add(new AudioDevice("线路输入", "line-in", true, false));
        }
        
        return devices;
    }
    
    /**
     * 设置默认音频输出设备（Windows）
     */
    public static boolean setDefaultOutputDevice(AudioDevice device) {
        if (!Platform.isWindows()) {
            return false;
        }
        
        try {
            // 使用PowerShell命令设置默认音频设备
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-Command",
                "Get-AudioDevice -List | Where-Object {$_.Name -like '*" + device.name + "*'} | Set-AudioDevice"
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("设置默认输出设备失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 设置默认音频输入设备（Windows）
     */
    public static boolean setDefaultInputDevice(AudioDevice device) {
        if (!Platform.isWindows()) {
            return false;
        }
        
        try {
            // 使用PowerShell命令设置默认音频设备
            ProcessBuilder pb = new ProcessBuilder(
                "powershell", "-Command",
                "Get-AudioDevice -List | Where-Object {$_.Name -like '*" + device.name + "*' -and $_.Type -eq 'Recording'} | Set-AudioDevice"
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            System.err.println("设置默认输入设备失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取当前默认输出设备
     */
    public static AudioDevice getCurrentDefaultOutputDevice() {
        List<AudioDevice> devices = getOutputDevices();
        return devices.stream()
                .filter(device -> device.isDefault)
                .findFirst()
                .orElse(devices.isEmpty() ? null : devices.get(0));
    }
    
    /**
     * 获取当前默认输入设备
     */
    public static AudioDevice getCurrentDefaultInputDevice() {
        List<AudioDevice> devices = getInputDevices();
        return devices.stream()
                .filter(device -> device.isDefault)
                .findFirst()
                .orElse(devices.isEmpty() ? null : devices.get(0));
    }
    
    /**
     * 测试音频输出设备
     */
    public static boolean testOutputDevice(AudioDevice device) {
        try {
            // 查找指定的音频设备
            Mixer targetMixer = null;
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            
            for (Mixer.Info mixerInfo : mixerInfos) {
                if (mixerInfo.getName().equals(device.name) || 
                    mixerInfo.getDescription().equals(device.id)) {
                    targetMixer = AudioSystem.getMixer(mixerInfo);
                    break;
                }
            }
            
            // 如果没找到指定设备，使用默认设备
            if (targetMixer == null) {
                targetMixer = AudioSystem.getMixer(null); // 默认混音器
            }
            
            // 创建音频格式
            AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            
            // 从指定的混音器获取音频线路
            if (targetMixer.isLineSupported(info)) {
                SourceDataLine line = (SourceDataLine) targetMixer.getLine(info);
                line.open(format);
                line.start();
                
                // 生成0.5秒的440Hz测试音调
                int sampleRate = 44100;
                int duration = sampleRate / 2; // 0.5秒
                int channels = 2; // 立体声
                int bytesPerSample = 2; // 16位
                int bufferSize = duration * channels * bytesPerSample;
                
                byte[] buffer = new byte[bufferSize];
                
                for (int i = 0; i < duration; i++) {
                    // 生成440Hz正弦波
                    double angle = 2.0 * Math.PI * i * 440.0 / sampleRate;
                    short sample = (short) (Math.sin(angle) * 16383); // 50%音量
                    
                    // 左声道
                    int pos = i * 4;
                    if (pos + 3 < buffer.length) {
                        buffer[pos] = (byte) (sample & 0xFF);
                        buffer[pos + 1] = (byte) ((sample >> 8) & 0xFF);
                        // 右声道
                        buffer[pos + 2] = (byte) (sample & 0xFF);
                        buffer[pos + 3] = (byte) ((sample >> 8) & 0xFF);
                    }
                }
                
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
                return true;
            }
        } catch (Exception e) {
            System.err.println("测试音频设备失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 获取音频设备详细信息
     */
    public static String getDeviceInfo(AudioDevice device) {
        StringBuilder info = new StringBuilder();
        info.append("设备名称: ").append(device.name).append("\n");
        info.append("设备ID: ").append(device.id).append("\n");
        info.append("类型: ").append(device.isInput ? "输入设备" : "输出设备").append("\n");
        info.append("默认设备: ").append(device.isDefault ? "是" : "否").append("\n");
        
        try {
            // 获取支持的音频格式
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            for (Mixer.Info mixerInfo : mixerInfos) {
                if (mixerInfo.getName().equals(device.name)) {
                    Mixer mixer = AudioSystem.getMixer(mixerInfo);
                    info.append("制造商: ").append(mixerInfo.getVendor()).append("\n");
                    info.append("版本: ").append(mixerInfo.getVersion()).append("\n");
                    break;
                }
            }
        } catch (Exception e) {
            info.append("无法获取详细信息\n");
        }
        
        return info.toString();
    }
    
    /**
     * 获取指定设备的混音器
     */
    public static Mixer getMixerForDevice(AudioDevice device) {
        try {
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            
            for (Mixer.Info mixerInfo : mixerInfos) {
                if (mixerInfo.getName().equals(device.name) || 
                    mixerInfo.getDescription().equals(device.id)) {
                    return AudioSystem.getMixer(mixerInfo);
                }
            }
        } catch (Exception e) {
            System.err.println("获取混音器失败: " + e.getMessage());
        }
        
        // 返回默认混音器
        try {
            return AudioSystem.getMixer(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 在指定设备上播放音频文件
     */
    public static boolean playAudioOnDevice(AudioDevice device, String audioFilePath) {
        // 注意：JavaFX MediaPlayer 不支持指定输出设备
        // 这里提供一个使用Java Sound API的替代方案
        System.out.println("JavaFX MediaPlayer 不支持指定输出设备");
        System.out.println("当前选择的设备: " + device.name);
        System.out.println("音频文件: " + audioFilePath);
        
        // 可以考虑使用其他音频库如 JavaZOOM 的 BasicPlayer
        // 或者通过 JNI 调用系统 API
        return false;
    }
}
