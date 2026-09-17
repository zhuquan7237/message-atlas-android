package com.messageatlas.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromAction(value: AppRuleAction): String = value.name
    @TypeConverter fun toAction(value: String): AppRuleAction = AppRuleAction.valueOf(value)
}

