package com.leetcrawler.model

data class LeetCodeProblem(
    val id: Int,
    val title: String,
    val titleSlug: String,
    val difficulty: String,
    val content: String,
    val codeSnippets: List<CodeSnippet>,
    val sampleTestCase: String? = null,
    val exampleTestcases: String? = null,
    val hints: List<String> = emptyList(),
    val similarQuestions: List<String> = emptyList(),
    val topicTags: List<TopicTag> = emptyList(),
    val stats: String? = null
)

data class CodeSnippet(
    val lang: String,
    val langSlug: String,
    val code: String
)

data class TopicTag(
    val name: String,
    val slug: String
)

data class LeetCodeSolution(
    val id: String,
    val title: String,
    val content: String,
    val lang: String,
    val langSlug: String,
    val author: String,
    val votes: Int
) 