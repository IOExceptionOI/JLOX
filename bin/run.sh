#!/bin/bash

# 定义路径
SRC_DIR="src"
OUT_DIR="out"
MAIN_CLASS="com.craftinginterpreters.lox.Lox"

# 确保输出目录存在
mkdir -p "$OUT_DIR"

# 编译所有 Java 文件
javac -d "$OUT_DIR" -encoding utf8 -sourcepath "$SRC_DIR" $(find "$SRC_DIR" -name "*.java")

# 检查编译是否成功
if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    
    # 运行 Lox 解释器
    java -cp "$OUT_DIR" "$MAIN_CLASS"

    if [ $? -eq 0 ]; then
        echo "Execution successful!"
    else
        echo "Execution failed!"
        exit 1
    fi
else
    echo "Compilation failed!"
    exit 1
fi

