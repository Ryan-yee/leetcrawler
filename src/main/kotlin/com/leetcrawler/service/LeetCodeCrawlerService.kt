package com.leetcrawler.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.leetcrawler.model.CodeSnippet
import com.leetcrawler.model.LeetCodeProblem
import com.leetcrawler.model.LeetCodeSolution
import com.leetcrawler.model.TopicTag
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.CompletableFuture

class LeetCodeCrawlerService {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val logger = Logger.getInstance(LeetCodeCrawlerService::class.java)
    
    companion object {
        private const val LEETCODE_BASE_URL = "https://leetcode.com"
        private const val LEETCODE_CN_BASE_URL = "https://leetcode.cn"
        private const val GRAPHQL_URL = "/graphql"
        
        fun getInstance(): LeetCodeCrawlerService {
            return ApplicationManager.getApplication().getService(LeetCodeCrawlerService::class.java)
        }
    }
    
    fun searchProblems(keyword: String, useChinese: Boolean = true): CompletableFuture<List<LeetCodeProblem>> {
        return CompletableFuture.supplyAsync {
            try {
                val baseUrl = if (useChinese) LEETCODE_CN_BASE_URL else LEETCODE_BASE_URL
                val problems = mutableListOf<LeetCodeProblem>()
                
                // 使用正确的 GraphQL 查询格式
                val searchQuery = """
                    query problemsetQuestionList(${'$'}categorySlug: String, ${'$'}limit: Int, ${'$'}skip: Int, ${'$'}filters: QuestionListFilterInput) {
                        problemsetQuestionList: questionList(
                            categorySlug: ${'$'}categorySlug
                            limit: ${'$'}limit
                            skip: ${'$'}skip
                            filters: ${'$'}filters
                        ) {
                            total: totalNum
                            questions: data {
                                acRate
                                difficulty
                                freqBar
                                frontendQuestionId: questionFrontendId
                                isFavor
                                paidOnly: isPaidOnly
                                status
                                title
                                titleSlug
                                topicTags {
                                    name
                                    id
                                    slug
                                }
                                hasSolution
                                hasVideoSolution
                            }
                        }
                    }
                """.trimIndent()
                
                // 使用正确的变量格式
                val variables = JsonObject().apply {
                    addProperty("categorySlug", "")
                    addProperty("skip", 0)
                    addProperty("limit", 20)
                    add("filters", JsonObject().apply {
                        addProperty("searchKeywords", keyword)
                    })
                }
                
                val requestBody = JsonObject().apply {
                    addProperty("query", searchQuery)
                    add("variables", variables)
                }
                
                logger.info("发送搜索请求: $keyword")
                logger.info("请求体: ${requestBody}")
                
                val request = Request.Builder()
                    .url("$baseUrl$GRAPHQL_URL")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Referer", baseUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                logger.info("响应状态: ${response.code}")
                logger.info("响应体: $responseBody")
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                    
                    if (jsonResponse.has("data") && 
                        jsonResponse.getAsJsonObject("data").has("problemsetQuestionList")) {
                        
                        val questionsArray = jsonResponse
                            .getAsJsonObject("data")
                            .getAsJsonObject("problemsetQuestionList")
                            .getAsJsonArray("questions")
                        
                        logger.info("找到 ${questionsArray.size()} 个题目")
                        
                        questionsArray.take(5).forEach { questionElement ->
                            val question = questionElement.asJsonObject
                            val titleSlug = question.get("titleSlug").asString
                            val title = question.get("title").asString
                            val difficulty = question.get("difficulty").asString
                            val questionId = question.get("frontendQuestionId").asInt
                            
                            logger.info("处理题目: $title")
                            
                            // 获取详细信息
                            getProblemDetail(titleSlug, useChinese)?.let { problem ->
                                problems.add(problem)
                            } ?: run {
                                // 如果获取详细信息失败，创建简化版本
                                val simpleProblem = LeetCodeProblem(
                                    id = questionId,
                                    title = title,
                                    titleSlug = titleSlug,
                                    difficulty = difficulty,
                                    content = "题目详情获取失败，请查看 LeetCode 网站",
                                    codeSnippets = emptyList(),
                                    topicTags = emptyList()
                                )
                                problems.add(simpleProblem)
                            }
                        }
                    } else {
                        logger.warn("响应中没有找到期望的数据结构")
                    }
                } else {
                    logger.error("搜索请求失败: ${response.code} - ${response.message}")
                }
                
                problems
            } catch (e: Exception) {
                logger.error("搜索题目失败", e)
                emptyList()
            }
        }
    }
    
    fun getProblemDetail(titleSlug: String, useChinese: Boolean = true): LeetCodeProblem? {
        return try {
            val baseUrl = if (useChinese) LEETCODE_CN_BASE_URL else LEETCODE_BASE_URL
            
            val query = """
                query questionData(${'$'}titleSlug: String!) {
                    question(titleSlug: ${'$'}titleSlug) {
                        questionId
                        questionFrontendId
                        title
                        titleSlug
                        content
                        difficulty
                        likes
                        dislikes
                        isLiked
                        similarQuestions
                        exampleTestcases
                        categoryTitle
                        topicTags {
                            name
                            slug
                            translatedName
                            __typename
                        }
                        codeSnippets {
                            lang
                            langSlug
                            code
                            __typename
                        }
                        stats
                        hints
                        status
                        sampleTestCase
                        metaData
                        judgerAvailable
                        judgeType
                        enableRunCode
                        enableTestMode
                        enableDebugger
                        envInfo
                        __typename
                    }
                }
            """.trimIndent()
            
            val variables = JsonObject().apply {
                addProperty("titleSlug", titleSlug)
            }
            
            val requestBody = JsonObject().apply {
                addProperty("query", query)
                add("variables", variables)
            }
            
            val request = Request.Builder()
                .url("$baseUrl$GRAPHQL_URL")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .addHeader("Referer", baseUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                
                if (jsonResponse.has("data") && 
                    jsonResponse.getAsJsonObject("data").has("question") &&
                    !jsonResponse.getAsJsonObject("data").get("question").isJsonNull) {
                    
                    val question = jsonResponse
                        .getAsJsonObject("data")
                        .getAsJsonObject("question")
                    
                    val codeSnippets = question.getAsJsonArray("codeSnippets")?.map { element ->
                        val snippet = element.asJsonObject
                        CodeSnippet(
                            lang = snippet.get("lang").asString,
                            langSlug = snippet.get("langSlug").asString,
                            code = snippet.get("code").asString
                        )
                    } ?: emptyList()
                    
                    val topicTags = question.getAsJsonArray("topicTags")?.map { element ->
                        val tag = element.asJsonObject
                        TopicTag(
                            name = tag.get("name").asString,
                            slug = tag.get("slug").asString
                        )
                    } ?: emptyList()
                    
                    LeetCodeProblem(
                        id = question.get("questionFrontendId").asInt,
                        title = question.get("title").asString,
                        titleSlug = question.get("titleSlug").asString,
                        difficulty = question.get("difficulty").asString,
                        content = question.get("content").asString,
                        codeSnippets = codeSnippets,
                        sampleTestCase = question.get("sampleTestCase")?.asString,
                        exampleTestcases = question.get("exampleTestcases")?.asString,
                        hints = question.getAsJsonArray("hints")?.map { it.asString } ?: emptyList(),
                        topicTags = topicTags,
                        stats = question.get("stats")?.asString
                    )
                } else {
                    logger.warn("题目详情响应中没有找到问题数据: $titleSlug")
                    null
                }
            } else {
                logger.error("获取题目详情请求失败: ${response.code} - $titleSlug")
                null
            }
        } catch (e: Exception) {
            logger.error("获取题目详情失败: $titleSlug", e)
            null
        }
    }
    
    fun getPopularProblems(useChinese: Boolean = true, page: Int = 0, limit: Int = 20): CompletableFuture<List<LeetCodeProblem>> {
        return CompletableFuture.supplyAsync {
            try {
                val baseUrl = if (useChinese) LEETCODE_CN_BASE_URL else LEETCODE_BASE_URL
                val problems = mutableListOf<LeetCodeProblem>()
                val skip = page * limit
                
                // 针对不同站点使用不同的查询格式
                val query = if (useChinese) {
                    // 中文站点使用最简化的查询
                    """
                    query problemsetQuestionList(${'$'}categorySlug: String, ${'$'}limit: Int, ${'$'}skip: Int) {
                        problemsetQuestionList: questionList(
                            categorySlug: ${'$'}categorySlug
                            limit: ${'$'}limit
                            skip: ${'$'}skip
                        ) {
                            total: totalNum
                            questions: data {
                                difficulty
                                frontendQuestionId: questionFrontendId
                                paidOnly: isPaidOnly
                                title
                                titleSlug
                                topicTags {
                                    name
                                    slug
                                }
                            }
                        }
                    }
                    """.trimIndent()
                } else {
                    // 国际站点使用完整的查询
                    """
                    query problemsetQuestionList(${'$'}categorySlug: String, ${'$'}limit: Int, ${'$'}skip: Int, ${'$'}filters: QuestionListFilterInput) {
                        problemsetQuestionList: questionList(
                            categorySlug: ${'$'}categorySlug
                            limit: ${'$'}limit
                            skip: ${'$'}skip
                            filters: ${'$'}filters
                        ) {
                            total: totalNum
                            questions: data {
                                acRate
                                difficulty
                                freqBar
                                frontendQuestionId: questionFrontendId
                                isFavor
                                paidOnly: isPaidOnly
                                status
                                title
                                titleSlug
                                topicTags {
                                    name
                                    id
                                    slug
                                }
                                hasSolution
                                hasVideoSolution
                            }
                        }
                    }
                    """.trimIndent()
                }
                
                val variables = if (useChinese) {
                    // CN站点使用简化的变量
                    JsonObject().apply {
                        addProperty("categorySlug", "")
                        addProperty("skip", skip)
                        addProperty("limit", limit)
                    }
                } else {
                    // 国际站可以使用filters
                    JsonObject().apply {
                        addProperty("categorySlug", "")
                        addProperty("skip", skip)
                        addProperty("limit", limit)
                        add("filters", JsonObject())
                    }
                }
                
                val requestBody = JsonObject().apply {
                    addProperty("query", query)
                    add("variables", variables)
                }
                
                logger.info("获取热门题目 - 页码: $page, 每页: $limit, 跳过: $skip, 站点: ${if (useChinese) "CN" else "COM"}")
                logger.info("请求体: ${requestBody}")
                
                val request = Request.Builder()
                    .url("$baseUrl$GRAPHQL_URL")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Referer", baseUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", if (useChinese) "zh-CN,zh;q=0.9" else "en-US,en;q=0.9")
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                logger.info("热门题目响应状态: ${response.code}")
                if (responseBody?.length ?: 0 > 1000) {
                    logger.info("响应体前1000字符: ${responseBody?.take(1000)}")
                } else {
                    logger.info("完整响应体: $responseBody")
                }
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                    
                    if (jsonResponse.has("data") &&
                        jsonResponse.getAsJsonObject("data").has("problemsetQuestionList") &&
                        !jsonResponse.getAsJsonObject("data").get("problemsetQuestionList").isJsonNull) {
                        
                        val questionsArray = jsonResponse
                            .getAsJsonObject("data")
                            .getAsJsonObject("problemsetQuestionList")
                            .getAsJsonArray("questions")
                        
                        logger.info("第 ${page + 1} 页找到 ${questionsArray.size()} 个题目")
                        
                        questionsArray.forEach { questionElement ->
                            val question = questionElement.asJsonObject
                            val titleSlug = question.get("titleSlug").asString
                            val title = question.get("title").asString
                            val difficulty = question.get("difficulty").asString
                            val questionId = question.get("frontendQuestionId").asInt
                            
                            // 为了提高性能，先创建简化版本，用户点击时再获取详细信息
                            val topicTags = question.getAsJsonArray("topicTags")?.map { element ->
                                val tag = element.asJsonObject
                                TopicTag(
                                    name = tag.get("name").asString,
                                    slug = tag.get("slug").asString
                                )
                            } ?: emptyList()
                            
                            val simpleProblem = LeetCodeProblem(
                                id = questionId,
                                title = title,
                                titleSlug = titleSlug,
                                difficulty = difficulty,
                                content = "点击查看详情", // 延迟加载
                                codeSnippets = emptyList(), // 延迟加载
                                topicTags = topicTags
                            )
                            problems.add(simpleProblem)
                        }
                    } else {
                        logger.warn("热门题目响应中没有找到期望的数据结构")
                        if (jsonResponse.has("errors")) {
                            val errors = jsonResponse.getAsJsonArray("errors")
                            logger.error("GraphQL 错误: $errors")
                        }
                    }
                } else {
                    logger.error("获取热门题目请求失败: ${response.code} - ${response.message}")
                    if (responseBody != null) {
                        logger.error("错误响应体: $responseBody")
                    }
                }
                
                problems
            } catch (e: Exception) {
                logger.error("获取热门题目失败", e)
                emptyList()
            }
        }
    }
    
    // 新增方法：搜索时也支持分页
    fun searchProblemsWithPaging(keyword: String, useChinese: Boolean = true, page: Int = 0, limit: Int = 20): CompletableFuture<List<LeetCodeProblem>> {
        return CompletableFuture.supplyAsync {
            try {
                val baseUrl = if (useChinese) LEETCODE_CN_BASE_URL else LEETCODE_BASE_URL
                val problems = mutableListOf<LeetCodeProblem>()
                val skip = page * limit
                
                // 针对中文站点，使用稍微不同的查询方式
                val searchQuery = if (useChinese) {
                    """
                    query problemsetQuestionList(${'$'}categorySlug: String, ${'$'}limit: Int, ${'$'}skip: Int, ${'$'}filters: QuestionListFilterInput) {
                        problemsetQuestionList: questionList(
                            categorySlug: ${'$'}categorySlug
                            limit: ${'$'}limit
                            skip: ${'$'}skip
                            filters: ${'$'}filters
                        ) {
                            total: totalNum
                            questions: data {
                                difficulty
                                frontendQuestionId: questionFrontendId
                                paidOnly: isPaidOnly
                                status
                                title
                                titleSlug
                                topicTags {
                                    name
                                    slug
                                    translatedName
                                }
                            }
                        }
                    }
                    """.trimIndent()
                } else {
                    """
                    query problemsetQuestionList(${'$'}categorySlug: String, ${'$'}limit: Int, ${'$'}skip: Int, ${'$'}filters: QuestionListFilterInput) {
                        problemsetQuestionList: questionList(
                            categorySlug: ${'$'}categorySlug
                            limit: ${'$'}limit
                            skip: ${'$'}skip
                            filters: ${'$'}filters
                        ) {
                            total: totalNum
                            questions: data {
                                acRate
                                difficulty
                                freqBar
                                frontendQuestionId: questionFrontendId
                                isFavor
                                paidOnly: isPaidOnly
                                status
                                title
                                titleSlug
                                topicTags {
                                    name
                                    id
                                    slug
                                }
                                hasSolution
                                hasVideoSolution
                            }
                        }
                    }
                    """.trimIndent()
                }
                
                val variables = JsonObject().apply {
                    addProperty("categorySlug", "")
                    addProperty("skip", skip)
                    addProperty("limit", limit)
                    add("filters", JsonObject().apply {
                        addProperty("searchKeywords", keyword)
                    })
                }
                
                val requestBody = JsonObject().apply {
                    addProperty("query", searchQuery)
                    add("variables", variables)
                }
                
                logger.info("搜索题目: $keyword - 页码: $page, 站点: ${if (useChinese) "CN" else "COM"}")
                
                val request = Request.Builder()
                    .url("$baseUrl$GRAPHQL_URL")
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Referer", baseUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    .addHeader("Accept", "application/json")
                    .addHeader("Accept-Language", if (useChinese) "zh-CN,zh;q=0.9" else "en-US,en;q=0.9")
                    .build()
                
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                logger.info("搜索响应状态: ${response.code}")
                if (responseBody?.length ?: 0 > 1000) {
                    logger.info("搜索响应体前1000字符: ${responseBody?.take(1000)}")
                } else {
                    logger.info("搜索响应体: $responseBody")
                }
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
                    
                    if (jsonResponse.has("data") && 
                        jsonResponse.getAsJsonObject("data").has("problemsetQuestionList")) {
                        
                        val questionsArray = jsonResponse
                            .getAsJsonObject("data")
                            .getAsJsonObject("problemsetQuestionList")
                            .getAsJsonArray("questions")
                        
                        logger.info("搜索找到 ${questionsArray.size()} 个题目")
                        
                        questionsArray.forEach { questionElement ->
                            val question = questionElement.asJsonObject
                            val titleSlug = question.get("titleSlug").asString
                            val title = question.get("title").asString
                            val difficulty = question.get("difficulty").asString
                            val questionId = question.get("frontendQuestionId").asInt
                            
                            val topicTags = question.getAsJsonArray("topicTags")?.map { element ->
                                val tag = element.asJsonObject
                                TopicTag(
                                    name = tag.get("name").asString,
                                    slug = tag.get("slug").asString
                                )
                            } ?: emptyList()
                            
                            val simpleProblem = LeetCodeProblem(
                                id = questionId,
                                title = title,
                                titleSlug = titleSlug,
                                difficulty = difficulty,
                                content = "点击查看详情", // 延迟加载
                                codeSnippets = emptyList(), // 延迟加载
                                topicTags = topicTags
                            )
                            problems.add(simpleProblem)
                        }
                    } else {
                        logger.warn("搜索响应中没有找到期望的数据结构")
                        if (jsonResponse.has("errors")) {
                            val errors = jsonResponse.getAsJsonArray("errors")
                            logger.error("GraphQL 错误: $errors")
                        }
                    }
                } else {
                    logger.error("搜索请求失败: ${response.code} - ${response.message}")
                }
                
                problems
            } catch (e: Exception) {
                logger.error("搜索题目失败", e)
                emptyList()
            }
        }
    }
} 