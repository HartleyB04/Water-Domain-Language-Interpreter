#!/bin/bash
# Compile script for Flow Interpreter with Dams

echo "Compiling Flow Interpreter..."

# Create build directories if they don't exist
mkdir -p build/classes

# Compile all Java files from flowInterpreter
javac -d build/classes riverFlow/flowInterpreter/*.java

# Create JAR file from compiled classes
jar cfe build/lox.jar flowInterpreter.Lox -C build/classes .

if [ $? -eq 0 ]; then
    echo "Creating JAR file..."
    # Create JAR file from compiled classes
    jar cfe build/lox.jar flowInterpreter.Lox -C build/classes .
    
    if [ $? -eq 0 ]; then
        echo "✓ Compilation successful!"
        echo ""
        echo "To run Demo Programs:"
        echo "  java -jar build/lox.jar 'Demo Programs'/program_1.lox"
        echo "  java -jar build/lox.jar 'Demo Programs'/program_2.lox"
        echo "  java -jar build/lox.jar 'Demo Programs'/program_3.lox"
        echo "  java -jar build/lox.jar 'Demo Programs'/program_4.lox"
    else
        echo "✗ JAR creation failed!"
        exit 1
    fi
else
    echo "✗ Compilation failed!"
    exit 1
fi


