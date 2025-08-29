package app;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

/**
 * 自定义音频播放器 - 支持指定输出设备
 */
public class CustomAudioPlayer {
    private Clip audioClip;
    private AudioDeviceManager.AudioDevice outputDevice;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    
    public CustomAudioPlayer(AudioDeviceManager.AudioDevice outputDevice) {
        this.outputDevice = outputDevice;
    }
    
    /**
     * 加载音频文件
     */
    public boolean loadAudio(File audioFile) {
        try {
            // 获取音频输入流
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();
            
            // 获取指定设备的混音器
            Mixer targetMixer = AudioDeviceManager.getMixerForDevice(outputDevice);
            if (targetMixer == null) {
                System.err.println("无法获取指定设备的混音器");
                return false;
            }
            
            // 创建 Clip 信息
            DataLine.Info info = new DataLine.Info(Clip.class, format);
            
            // 检查设备是否支持该格式
            if (!targetMixer.isLineSupported(info)) {
                System.err.println("设备不支持该音频格式: " + format);
                // 尝试转换格式
                AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    format.getChannels(),
                    format.getChannels() * 2,
                    format.getSampleRate(),
                    false
                );
                
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                format = targetFormat;
                info = new DataLine.Info(Clip.class, format);
                
                if (!targetMixer.isLineSupported(info)) {
                    System.err.println("设备不支持转换后的音频格式");
                    return false;
                }
            }
            
            // 从指定混音器获取 Clip
            audioClip = (Clip) targetMixer.getLine(info);
            audioClip.open(audioInputStream);
            
            System.out.println("音频已加载到设备: " + outputDevice.name);
            return true;
            
        } catch (UnsupportedAudioFileException e) {
            System.err.println("不支持的音频文件格式: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("读取音频文件失败: " + e.getMessage());
        } catch (LineUnavailableException e) {
            System.err.println("音频线路不可用: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("加载音频失败: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * 播放音频
     */
    public void play() {
        if (audioClip != null) {
            if (isPaused) {
                // 从暂停位置继续播放
                audioClip.start();
                isPaused = false;
            } else {
                // 从头开始播放
                audioClip.setFramePosition(0);
                audioClip.start();
            }
            isPlaying = true;
            System.out.println("开始播放音频到设备: " + outputDevice.name);
        }
    }
    
    /**
     * 暂停音频
     */
    public void pause() {
        if (audioClip != null && isPlaying) {
            audioClip.stop();
            isPlaying = false;
            isPaused = true;
            System.out.println("音频已暂停");
        }
    }
    
    /**
     * 停止音频
     */
    public void stop() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.setFramePosition(0);
            isPlaying = false;
            isPaused = false;
            System.out.println("音频已停止");
        }
    }
    
    /**
     * 设置音量 (0.0 - 1.0)
     */
    public void setVolume(double volume) {
        if (audioClip != null) {
            try {
                FloatControl volumeControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                float gain = (float) (Math.log(Math.max(0.01, volume)) / Math.log(10.0) * 20.0);
                volumeControl.setValue(Math.max(volumeControl.getMinimum(), Math.min(gain, volumeControl.getMaximum())));
            } catch (Exception e) {
                System.err.println("设置音量失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取播放状态
     */
    public boolean isPlaying() {
        return isPlaying && audioClip != null && audioClip.isRunning();
    }
    
    /**
     * 获取是否暂停
     */
    public boolean isPaused() {
        return isPaused;
    }
    
    /**
     * 获取音频长度（秒）
     */
    public double getDuration() {
        if (audioClip != null) {
            return audioClip.getMicrosecondLength() / 1_000_000.0;
        }
        return 0;
    }
    
    /**
     * 获取当前播放位置（秒）
     */
    public double getCurrentTime() {
        if (audioClip != null) {
            return audioClip.getMicrosecondPosition() / 1_000_000.0;
        }
        return 0;
    }
    
    /**
     * 设置播放位置（秒）
     */
    public void setCurrentTime(double seconds) {
        if (audioClip != null) {
            long microseconds = (long) (seconds * 1_000_000);
            audioClip.setMicrosecondPosition(Math.max(0, Math.min(microseconds, audioClip.getMicrosecondLength())));
        }
    }
    
    /**
     * 释放资源
     */
    public void dispose() {
        if (audioClip != null) {
            audioClip.close();
            audioClip = null;
        }
        isPlaying = false;
        isPaused = false;
    }
    
    /**
     * 更换输出设备
     */
    public void setOutputDevice(AudioDeviceManager.AudioDevice newDevice) {
        boolean wasPlaying = isPlaying();
        double currentTime = getCurrentTime();
        
        // 保存当前状态
        dispose();
        
        // 设置新设备
        this.outputDevice = newDevice;
        
        // 如果之前在播放，需要重新加载并继续播放
        // 注意：这需要调用者重新调用 loadAudio
        System.out.println("输出设备已更换为: " + newDevice.name);
    }
    

}
