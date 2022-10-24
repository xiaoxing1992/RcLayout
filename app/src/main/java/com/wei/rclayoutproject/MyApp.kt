package com.wei.rclayoutproject

import android.app.Application
import skin.support.SkinCompatManager
import skin.support.app.SkinAppCompatViewInflater
import skin.support.app.SkinCardViewInflater
import skin.support.constraint.app.SkinConstraintViewInflater
import skin.support.design.app.SkinMaterialViewInflater

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()
        SkinCompatManager.withoutActivity(this)// 基础控件换肤初始化
            //.addStrategy(MBSkinSDCardLoader())
            .addInflater(SkinMBViewInflater()) // 自定义
            .addInflater(SkinConstraintViewInflater()) // ConstraintLayout 控件换肤初始化[可选]
            .addInflater(SkinMaterialViewInflater()) // material design 控件换肤初始化[可选]
            .addInflater(SkinAppCompatViewInflater()) // material design 控件换肤初始化[可选]
            .setSkinWindowBackgroundEnable(false) // 关闭windowBackground换肤，默认打开[可选]
            .loadSkin()
    }

}