package com.example.jetnews.data.preferences

/** Guardian `/sections` 接口实际返回的、可用于推荐的新闻类别。 */
enum class NewsInterest(
    val sectionId: String,
    val displayName: String,
    val description: String,
) {
    TECHNOLOGY("technology", "科技", "数字产品、互联网与创新"),
    SCIENCE("science", "科学", "研究、太空与科学发现"),
    WORLD("world", "世界", "全球新闻与国际动态"),
    BUSINESS("business", "商业", "公司、市场与经济"),
    ENVIRONMENT("environment", "环境", "气候、自然与可持续发展"),
    SPORT("sport", "体育", "赛事、球队与运动员"),
    CULTURE("culture", "文化", "艺术、影视与书籍"),
    POLITICS("politics", "政治", "公共政策与政治新闻"),
    LIFE_AND_STYLE("lifeandstyle", "生活", "健康、饮食与生活方式"),
    ;

    companion object {
        val default = listOf(TECHNOLOGY, SCIENCE, WORLD)

        fun fromSectionId(sectionId: String): NewsInterest? =
            entries.firstOrNull { it.sectionId == sectionId }
    }
}
