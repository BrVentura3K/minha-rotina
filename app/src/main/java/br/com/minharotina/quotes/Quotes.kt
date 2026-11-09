package br.com.minharotina.quotes

import java.time.LocalDate
import kotlin.random.Random

object Quotes {

    val all: List<String> = listOf(
        "Constância vale mais que perfeição. Um dia de cada vez.",
        "Você não precisa fazer tudo hoje. Só precisa não desistir.",
        "Começar já é metade do caminho.",
        "Pequenos passos todo dia viram grandes mudanças.",
        "Feito é melhor que perfeito.",
        "Hoje é um bom dia pra cuidar de você.",
        "Cada tarefa ticada é uma promessa cumprida com você mesma.",
        "Devagar também é andar pra frente.",
        "Seu eu do futuro vai agradecer pelo que você fizer hoje.",
        "Não espere vontade. Comece e a vontade aparece.",
        "Disciplina é lembrar do que você quer de verdade.",
        "Um passo pequeno ainda é um passo.",
        "Você já venceu dias mais difíceis que esse.",
        "Faça por você, não pela lista.",
        "O progresso mora na repetição.",
        "Respira fundo e vai uma tarefa de cada vez.",
        "Rotina boa é aquela que cabe na sua vida.",
        "Hoje conta. Mesmo que seja só um pouquinho.",
        "Errou ontem? Hoje é página nova.",
        "Seu ritmo é o ritmo certo.",
        "Cuidar de si também é produtividade.",
        "Comece pelo mais fácil e deixe o embalo te levar.",
        "Quem planta um pouco todo dia colhe muito depois.",
        "A melhor hora pra começar é agora.",
        "Você é capaz de coisas incríveis, inclusive terminar essa lista.",
        "Nada muda se nada muda. Bora mudar um pouquinho hoje.",
        "O difícil fica fácil com a prática.",
        "Foco no próximo passo, não na escada inteira.",
        "Sua força está em continuar.",
        "Você está construindo algo bonito, tijolo por tijolo.",
        "Hoje, faça o seu melhor possível. E isso basta.",
        "Organização é carinho com o seu tempo.",
        "Não precisa ser intenso, precisa ser frequente.",
        "Comemore as pequenas vitórias. Elas somam.",
        "Seja gentil consigo mesma durante o processo.",
        "Uma tarefa por vez, sem pressa e sem pausa longa.",
        "O hábito de hoje é a identidade de amanhã.",
        "Você não está atrasada. Está no seu tempo.",
        "Cansou? Descanse, mas não desista.",
        "Toda grande jornada é feita de dias comuns.",
        "Ter um plano já é um ótimo começo.",
        "Você merece a versão de você que cumpre o que promete.",
        "Mais um dia, mais uma chance de fazer acontecer.",
        "Os dias bons você constrói com as próprias mãos.",
        "Menos cobrança, mais constância.",
        "O que é feito com amor fica leve.",
        "Hoje você escolhe cuidar de você. Que bonito.",
        "Pequenas escolhas, grandes resultados.",
        "Siga em frente, mesmo que devagarinho.",
        "Seu esforço conta, mesmo quando ninguém está vendo.",
        "Às vezes, o primeiro check é tudo que você precisa.",
        "Você é mais forte do que a sua preguiça.",
        "A lista é sua aliada, não sua chefe.",
        "Faça hoje algo que te deixe orgulhosa à noite.",
        "Paciência: tudo que vale a pena leva tempo.",
        "Quem tem rotina tem mais liberdade.",
        "Seu progresso não precisa ser perfeito para ser real.",
        "Mente tranquila, tarefa cumprida.",
        "Vai com calma, mas vai.",
        "Hoje é dia de mostrar pra você do que você é capaz.",
        "A constância transforma o comum em extraordinário.",
        "Organize o dia e o dia te organiza.",
        "O sucesso é a soma de pequenos esforços repetidos.",
        "Ninguém faz tudo. Mas você pode fazer o seu.",
        "Que hoje seja leve e produtivo.",
        "Você já começou. Agora é só continuar.",
        "Acredite no processo, mesmo nos dias lentos.",
        "Uma rotina gentil é uma rotina que dura.",
        "Dia ruim não é vida ruim. Faz o que der.",
        "Você está indo muito melhor do que imagina.",
        "Cada dia cumprido é um voto na pessoa que você quer ser.",
        "Faça o simples bem feito.",
        "Seus hábitos dizem muito sobre o seu amor-próprio.",
        "Termine o dia sabendo que tentou.",
        "Não é sobre motivação, é sobre compromisso.",
        "Seu tempo é precioso. Use com carinho.",
        "Bora fazer acontecer, uma caixinha de cada vez.",
        "Pouco a pouco, você chega lá.",
        "A sua dedicação inspira quem está perto de você.",
        "Hoje tem tudo pra ser um ótimo dia.",
    )

    private val celebrate = listOf(
        "Dia completo! Você arrasou hoje.",
        "Tudo feito! Pode descansar com orgulho.",
        "Lista zerada! Que dia bonito você construiu.",
        "100% concluído. Você é incrível!",
        "Missão cumprida! Amanhã tem mais, e você dá conta.",
    )

    /** Frase do dia: muda à meia-noite e só se repete depois de passar por todas. */
    fun ofDay(date: LocalDate, custom: List<String>): String {
        val pool = all + custom
        val order = pool.indices.shuffled(Random(date.year))
        return pool[order[(date.dayOfYear - 1) % pool.size]]
    }

    data class Headline(val label: String, val text: String)

    /** Texto do cartão da tela Hoje, que reage ao progresso. */
    fun headline(date: LocalDate, done: Int, total: Int, custom: List<String>): Headline {
        val remaining = total - done
        return when {
            total == 0 -> Headline("Dia livre", "Nenhuma tarefa pra hoje. Aproveita pra descansar!")
            remaining == 0 -> Headline("Dia completo", celebrate[(date.toEpochDay() % celebrate.size).toInt()])
            done > 0 && remaining == 1 -> Headline("Quase lá", "Falta só 1. Você consegue!")
            done > 0 && remaining == 2 -> Headline("Quase lá", "Faltam só 2. Você já fez a parte mais difícil: começar.")
            else -> Headline("Frase do dia", ofDay(date, custom))
        }
    }
}
