package app;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

/**
 * 统一字体管理器
 * 自动处理所有UI组件的中文字体显示问题
 */
public class FontManager {
    
    // 中文字体样式
    private static final String CHINESE_FONT_STYLE = 
        "-fx-font-family: 'Microsoft YaHei', '微软雅黑', 'SimSun', '宋体', 'SimHei', '黑体', 'Arial Unicode MS', sans-serif;";
    
    /**
     * 应用中文字体到场景
     */
    public static void applyChineseFontToScene(Scene scene) {
        if (scene != null && scene.getRoot() != null) {
            // 设置根节点字体
            scene.getRoot().setStyle(CHINESE_FONT_STYLE);
            
            // 递归应用到所有子节点
            applyFontToNode(scene.getRoot());
        }
    }
    
    /**
     * 应用中文字体到舞台（包括所有对话框）
     */
    public static void applyChineseFontToStage(Stage stage) {
        if (stage != null && stage.getScene() != null) {
            applyChineseFontToScene(stage.getScene());
            
            // 监听场景变化
            stage.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    Platform.runLater(() -> applyChineseFontToScene(newScene));
                }
            });
        }
    }
    
    /**
     * 应用中文字体到对话框
     */
    public static void applyChineseFontToDialog(Dialog<?> dialog) {
        if (dialog != null && dialog.getDialogPane() != null) {
            DialogPane dialogPane = dialog.getDialogPane();
            
            // 设置对话框面板字体
            dialogPane.setStyle(CHINESE_FONT_STYLE);
            
            // 应用到对话框内容
            if (dialogPane.getContent() != null) {
                applyFontToNode(dialogPane.getContent());
            }
            
            // 应用到按钮
            for (ButtonType buttonType : dialogPane.getButtonTypes()) {
                Node button = dialogPane.lookupButton(buttonType);
                if (button != null) {
                    applyFontToNode(button);
                }
            }
            
            // 监听内容变化
            dialogPane.contentProperty().addListener((obs, oldContent, newContent) -> {
                if (newContent != null) {
                    Platform.runLater(() -> applyFontToNode(newContent));
                }
            });
        }
    }
    
    /**
     * 递归应用字体到节点及其所有子节点
     */
    public static void applyFontToNode(Node node) {
        if (node == null) return;
        
        try {
            // 检查样式属性是否被绑定
            if (!node.styleProperty().isBound()) {
                String currentStyle = node.getStyle();
                
                // 如果当前样式中没有字体设置，则添加中文字体
                if (currentStyle == null || !currentStyle.contains("-fx-font-family")) {
                    if (currentStyle == null || currentStyle.isEmpty()) {
                        node.setStyle(CHINESE_FONT_STYLE);
                    } else {
                        node.setStyle(currentStyle + "; " + CHINESE_FONT_STYLE);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略设置失败的情况
        }
        
        // 递归处理子节点
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                applyFontToNode(child);
            }
        }
        
        // 特殊处理一些控件的内部节点
        handleSpecialControls(node);
    }
    
    /**
     * 处理特殊控件的内部节点
     */
    private static void handleSpecialControls(Node node) {
        try {
            // 处理 ComboBox 的下拉列表
            if (node instanceof ComboBox<?> comboBox) {
                comboBox.setButtonCell(createStyledListCell());
                comboBox.setCellFactory(lv -> createStyledListCell());
            }
            
            // 处理 ListView 的单元格
            if (node instanceof ListView<?> listView) {
                listView.setCellFactory(lv -> createStyledListCell());
            }
            
            // 处理 TableView 的列
            if (node instanceof TableView<?> tableView) {
                tableView.getColumns().forEach(column -> {
                    if (column instanceof TableColumn<?, ?> tableColumn) {
                        tableColumn.setStyle(CHINESE_FONT_STYLE);
                    }
                });
            }
            
            // 处理 MenuBar 和 Menu
            if (node instanceof MenuBar menuBar) {
                for (Menu menu : menuBar.getMenus()) {
                    applyFontToMenu(menu);
                }
            }
            
            // ContextMenu 不是 Node 的子类，需要单独处理
            
        } catch (Exception e) {
            // 忽略处理失败的情况
        }
    }
    
    /**
     * 创建带样式的列表单元格
     */
    private static <T> ListCell<T> createStyledListCell() {
        return new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setStyle(CHINESE_FONT_STYLE);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString());
                }
            }
        };
    }
    
    /**
     * 应用字体到菜单
     */
    private static void applyFontToMenu(Menu menu) {
        try {
            // 设置菜单项字体
            menu.setStyle(CHINESE_FONT_STYLE);
            
            // 递归处理子菜单项
            for (MenuItem item : menu.getItems()) {
                item.setStyle(CHINESE_FONT_STYLE);
                if (item instanceof Menu subMenu) {
                    applyFontToMenu(subMenu);
                }
            }
        } catch (Exception e) {
            // 忽略设置失败的情况
        }
    }
    
    /**
     * 自动监听并应用字体到新创建的窗口
     */
    public static void enableAutoFontApplication() {
        // 监听所有新创建的 Stage
        Platform.runLater(() -> {
            try {
                // 这里可以添加全局监听器，但JavaFX没有直接的API
                // 所以我们采用在创建窗口时手动调用的方式
                System.out.println("字体管理器已启用，请在创建新窗口时调用 FontManager.applyChineseFontToStage()");
            } catch (Exception e) {
                System.err.println("启用自动字体应用失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 获取中文字体样式字符串
     */
    public static String getChineseFontStyle() {
        return CHINESE_FONT_STYLE;
    }
}
