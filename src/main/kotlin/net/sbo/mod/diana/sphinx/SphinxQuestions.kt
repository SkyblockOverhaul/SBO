package net.sbo.mod.diana.sphinx

object SphinxQuestions {
    data class SphinxQuestion(
        val question: String,
        val answer: String
    )

    val QUESTIONS: List<SphinxQuestion> = listOf(
        SphinxQuestion("Which of these is NOT a pet?", "Slime"),
        SphinxQuestion("What type of mob is exclusive to the Fishing Festival?", "Shark"),
        SphinxQuestion("Where is Trevor the Trapper found?", "Mushroom Desert"),
        SphinxQuestion("Who helps you apply Rod Parts?", "Roddy"),
        SphinxQuestion("Which type of Gemstone has the lowest Breaking Power?", "Ruby"),
        SphinxQuestion("Which item rarity comes after Mythic?", "Divine"),
        SphinxQuestion("How do you obtain the Dark Purple Dye?", "Dark Auction"),
        SphinxQuestion("Who runs the Chocolate Factory?", "Hoppity"),
        SphinxQuestion("How many floors are there in The Catacombs?", "7"),
        SphinxQuestion("What is the first type of slayer Maddox offers?", "Zombie"),
        SphinxQuestion("What item do you use to kill Pests?", "Vacuum"),
        SphinxQuestion("Who owns the Gold Essence Shop?", "Marigold"),
        SphinxQuestion("Which of these is NOT a type of Gemstone?", "Prismite"),
        SphinxQuestion("What does Junker Joel collect?", "Junk"),
        SphinxQuestion("Where is the Titanoboa found?", "Backwater Bayou")
    )

    val CORRECT_ANSWERS: Set<String> = QUESTIONS.map { it.answer }.toSet()
}

