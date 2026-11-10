package br.com.minharotina.quotes

import java.time.LocalDate
import kotlin.random.Random

object Quotes {

    val all: List<String> = listOf(
        "Detetive, a cena do crime é a pia da cozinha. Hora de resolver esse caso.",
        "Mais uma tarefa fechada. Você é basicamente a melhor detetive dessa delegacia.",
        "Caso aberto: a lista de hoje. Suspeita principal: a preguiça. Prenda ela.",
        "Bom dia, detetive! A delegacia conta com você hoje.",
        "Toda grande investigação começa com um café e um check.",
        "Essa lista não vai se resolver sozinha. Ainda bem que você é genial.",
        "Plano infalível: fazer uma tarefa, comemorar, repetir.",
        "Atenção, central: detetive em campo, tarefas em perigo.",
        "Hoje você não é só uma pessoa. Você é uma lenda em serviço.",
        "Nenhum caso é difícil demais para quem tem uma lista e uma boa trilha sonora.",
        "Missão do dia: ser incrível. Dificuldade: fácil, porque é você.",
        "Se procrastinar fosse crime, hoje você ia prender ela em flagrante.",
        "Relatório oficial: você está mandando muito bem.",
        "Detetive, as provas são claras: você consegue.",
        "Um check por vez, como uma investigação de verdade.",
        "A lista tremeu quando viu você chegando.",
        "Coloca os óculos escuros mentais. É hora de agir.",
        "Interrogatório rápido: quem vai arrasar hoje? Exatamente, você.",
        "O caso das tarefas pendentes está prestes a ser encerrado.",
        "Hoje o crime não compensa. Mas tarefa feita compensa muito.",
        "Nada de cena dramática: só foco e uma comemoração exagerada no final.",
        "Você não está atrasada, está fazendo uma entrada triunfal.",
        "Toda heroína de filme de ação começa o dia com uma lista. Confia.",
        "A delegacia inteira está torcendo por você. Bom, pelo menos eu estou.",
        "Pista do dia: a primeira tarefa é a mais fácil de pegar.",
        "Seu distintivo de produtividade está brilhando hoje.",
        "Detetive, permissão concedida para ser incrível.",
        "Essa rotina é sua parceira de patrulha. Cuida bem dela.",
        "Respira, ajeita o cabelo e vai resolver esse caso.",
        "Grande dia para fechar casos e comer um lanchinho de recompensa.",
        "Se a tarefa fugir, você persegue. Em câmera lenta, de preferência.",
        "Caso resolvido é caso comemorado. Faz a dancinha.",
        "Você tem o instinto de uma detetive e a energia de uma estrela de cinema.",
        "Alerta geral: alguém aqui está imparável hoje.",
        "A sua lista não sabe com quem se meteu.",
        "Prova número um: você já começou. Prova número dois: vai terminar.",
        "Hoje é dia de virar a capitã da própria rotina.",
        "Detetive, não tem caso arquivado com você por perto.",
        "Uma tarefa feita por dia mantém o caos longe da delegacia.",
        "Investigação concluída: você merece um descanso depois.",
        "Nada mais assustador para uma tarefa do que você focada.",
        "Vai com calma, mas vai como protagonista.",
        "Sua missão, caso aceite: ticar tudo e se sentir uma gênia.",
        "Cada check é uma medalha invisível no seu uniforme.",
        "Dia ruim? Até as maiores detetives têm. Faz o que der e já é vitória.",
        "Você é tipo um filme de ação, só que mais organizada.",
        "A vilã de hoje se chama Deixa Pra Depois. Não deixa ela vencer.",
        "Detetive, sua dedicação está sendo registrada nos autos.",
        "Ninguém resolve tudo de uma vez. Nem os melhores detetives.",
        "Fica tranquila: até o caso mais enrolado se resolve com paciência.",
        "Uma lista bem feita vale mais que mil planos mirabolantes.",
        "Hoje você tem licença oficial para se achar.",
        "Primeira regra da delegacia: nunca subestimar você.",
        "O plano é simples, a execução é brilhante e a detetive é você.",
        "Se tiver dúvida, faz a tarefa mais rápida e ganha embalo.",
        "Atenção: alto nível de competência detectado neste celular.",
        "Você está a algumas tarefas de virar a lenda do dia.",
        "Toda grande parceria começa assim: você e sua lista.",
        "Caso encerrado, detetive. Próximo!",
        "Ninguém para uma detetive com propósito.",
        "Seu talento para resolver as coisas é quase suspeito.",
        "Hoje vale até comemorar com trilha sonora épica.",
        "Siga as pistas: elas levam direto para um dia bem vivido.",
        "Uma detetive descansada resolve mais casos. Cuida de você também.",
        "Equipe reunida, plano traçado. Bora.",
        "Você é a prova viva de que constância dá resultado.",
        "Hora do relatório: dia produtivo, energia alta, carisma absurdo.",
        "Até o arquivo morto se anima quando você aparece.",
        "A cada check, a delegacia aplaude de pé.",
        "Grande detetive não é quem nunca falha, é quem volta no dia seguinte.",
        "Hoje ninguém sai daqui sem resolver pelo menos um caso.",
        "O segredo do sucesso: lista, foco e um pouquinho de drama.",
        "Com você no comando, essa rotina está em boas mãos.",
        "Primeira tarefa feita? Então já pode se considerar promovida.",
        "Ser incrível todo dia cansa, então faz uma pausa e volta brilhando.",
        "Detetive, a sua rotina é o melhor caso que você já montou.",
        "Hoje é um ótimo dia para impressionar a si mesma.",
        "Essa lista é só uma formalidade. Todo mundo sabe que você vai vencer.",
        "Os casos difíceis são sempre os mais legais de resolver.",
        "Você chegou, a lista ficou nervosa e o dia ficou melhor.",
    )

    private val celebrate = listOf(
        "Caso encerrado! Todas as tarefas resolvidas. Pode fazer a dancinha da vitória.",
        "Lista zerada. A delegacia declara: melhor detetive do dia.",
        "Investigação completa, zero pendências. Você é uma lenda.",
        "Tudo feito! Pode sair em câmera lenta com uma explosão ao fundo.",
        "Relatório final: 100% resolvido. Promoção imediata para você.",
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
            total == 0 -> Headline("Folga da delegacia", "Nenhum caso hoje, detetive. Aproveita pra descansar!")
            remaining == 0 -> Headline("Caso encerrado", celebrate[(date.toEpochDay() % celebrate.size).toInt()])
            done > 0 && remaining == 1 -> Headline("Quase lá", "Falta só 1 caso, detetive. Fecha esse e comemora!")
            done > 0 && remaining == 2 -> Headline("Quase lá", "Faltam só 2 casos. A investigação está quase concluída.")
            else -> Headline("Frase do dia", ofDay(date, custom))
        }
    }
}
