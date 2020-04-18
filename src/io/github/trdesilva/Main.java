package io.github.trdesilva;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        String url;
        if(args.length < 1)
        {
            System.out.println("no");
            return;
        }
        else if(args.length == 1 && (args[0].startsWith("http://") || args[0].startsWith("https://")))
        {
            url = args[0];
        }
        else
        {
            url = "https://na.op.gg/multi/query=";
            url += URLEncoder.encode(String.join(",", args), Charset.defaultCharset());
        }
        
        System.out.println("querying " + url);
        Document root = Jsoup.connect(url).get();
        StringBuilder tsv = new StringBuilder("Player\tRank\tStats From\tChamp 1\tChamp 2\tChamp 3\tChamp 4\tChamp 5\n");
        for(Element player: root.getElementsByClass("MultiSearchResultRow"))
        {
            tsv.append(getRow(player));
        }
        
        System.out.println(tsv);
        return;
    }
    
    private static String getRow(Element player) throws Exception
    {
        List<String> columns = new ArrayList<String>(8);
        
        String playerName = player.getElementsByClass("SummonerName").text();
        System.out.println("checking " + playerName);
        columns.add(playerName);
        String rank = player.getElementsByClass("TierRank").get(1).text();
        if(rank.startsWith("Level"))
        {
            Elements previousSeasonDiv = player.getElementsByClass("PreviousSeason");
            if(previousSeasonDiv != null && previousSeasonDiv.size() > 0)
            {
                Elements previousSeasonList = previousSeasonDiv.get(0).getElementsByTag("li");
                rank += String.format(" (%s)", previousSeasonList.get(previousSeasonList.size() - 1).text());
            }
        }
        columns.add(rank);
        
        Document playerChampPage = Jsoup.connect(getIndividualLink(playerName, true)).get();
        
        Element seasons = playerChampPage.getElementById("champion_season");
        columns.add(seasons.getElementsByClass("active").get(0).getElementsByTag("a").get(0).text());
        
        Elements mostRecentChampTable = playerChampPage.getElementsByClass("ChampionStatsBox").get(0)
                                                       .getElementsByClass("ChampionStatsTable").get(0)
                                                       .getElementsByClass("TopRanker");
        for(int i = 0; i < 5; i++)
        {
            if(i < mostRecentChampTable.size())
            {
                Element champRow = mostRecentChampTable.get(i);
                String champData = String.format("%s %s %s %s",
                                                 getChampName(champRow),
                                                 getWinLoss(champRow),
                                                 getKda(champRow),
                                                 getCs(champRow));
                columns.add(champData);
            }
            else
            {
                columns.add("N/A");
            }
        }

        return String.join("\t", columns) + "\n";
    }
    
    private static String getIndividualLink(String playerName, boolean championsTable)
    {
        return "https://na.op.gg/summoner/" + (championsTable ? "champions/" : "") + "userName=" + playerName;
    }
    
    private static String getChampName(Element champRow)
    {
        return champRow.getElementsByClass("ChampionName").get(0).attr("data-value");
    }
    
    private static String getWinLoss(Element champRow)
    {
        Element winRatioCell = champRow.getElementsByClass("WinRatioGraph").get(0);
        String stat1 = winRatioCell.getElementsByClass("Text").get(0).text();
        String stat2 = winRatioCell.getElementsByClass("Text").size() > 1 ?
                winRatioCell.getElementsByClass("Text").get(1).text() : "";
        String wins;
        String losses;
        // if they have 0% or 100% winrate, there will only be one Text element, so we can't use index to know which is which
        if(stat1.endsWith("W") && stat2.isBlank())
        {
            wins = stat1;
            losses = "0L";
        }
        else if(stat1.endsWith("L"))
        {
            wins = "0W";
            losses = stat1;
        }
        else
        {
            wins = stat1;
            losses = stat2;
        }
        return String.format("%s/%s (%s)",
                             wins,
                             losses,
                             winRatioCell.getElementsByClass("WinRatio").text());
    }
    
    private static String getKda(Element champRow)
    {
        Element kdaCell = champRow.getElementsByClass("KDA").get(0);
        return String.format("%s/%s/%s (%s:1)",
                             kdaCell.getElementsByClass("Kill").text(),
                             kdaCell.getElementsByClass("Death").text(),
                             kdaCell.getElementsByClass("Assist").text(),
                             champRow.getElementsByClass("KDA").get(0).attr("data-value"));
    }
    
    private static String getCs(Element champRow)
    {
        return champRow.getElementsByClass("Cell").get(6).text() + " CS";
    }
}
