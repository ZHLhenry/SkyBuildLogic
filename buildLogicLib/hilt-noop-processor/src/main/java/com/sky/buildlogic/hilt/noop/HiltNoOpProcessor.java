package com.sky.buildlogic.hilt.noop;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

/**
 * 无操作注解处理器，用于消除 Hilt Gradle 插件在 KSP 场景下向 javac 注入的
 * 仅用于 KAPT 的内部选项所导致的 "以下选项未被任何处理程序识别" 警告。
 *
 * <p>此处理器声明识别 Hilt 注入的所有编译器选项，但不执行任何实际处理逻辑。</p>
 */
@SupportedAnnotationTypes("*")
public final class HiltNoOpProcessor extends AbstractProcessor {

    /**
     * Hilt Gradle 插件可能注入的编译器选项（KAPT 场景）。
     * 不同 Hilt 版本可能注入不同的选项，这里列出已知的所有选项。
     */
    private static final Set<String> SUPPORTED_OPTIONS = Set.of(
            // Dagger 核心选项
            "dagger.fastInit",
            "dagger.experimentalDaggerErrorMessages",
            // Hilt Android 选项
            "dagger.hilt.android.internal.disableAnnotationSuperinterface",
            "dagger.hilt.android.internal.androidPackage",
            "dagger.hilt.android.internal.projectRootDir",
            "dagger.hilt.android.internal.aggregatingSubcomponentsEnabled",
            "dagger.hilt.android.internal.disableAndroidSuperclassValidation",
            "dagger.hilt.android.internal.projectType",
            "dagger.hilt.internal.useAggregatingRootProcessor"
    );

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // 无操作：仅用于声明识别 Hilt 的编译器选项以消除 javac 警告
        return false;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
