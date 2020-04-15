package io.github.trdesilva;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.nio.charset.Charset;

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
        StringBuilder tsv = new StringBuilder("Player\tRank\tChamp1\tChamp2\tChamp3\tChamp4\tChamp5\n");
        for(Element player: root.getElementsByClass("MultiSearchResultRow"))
        {
            tsv.append(getRow(player));
        }
        
        System.out.println(tsv);
        return;
    }
    
    private static String getRow(Element player)
    {
        StringBuilder row = new StringBuilder();
        
        row.append(player.getElementsByClass("SummonerName").text());
        row.append('\t');
        row.append(player.getElementsByClass("TierRank").get(1).text());
        row.append('\t');
        
        Elements mostRecentChampTable = player.getElementsByClass("ChampionSmallStats").get(0)
                                              .getElementsByClass("Content").get(0)
                                              .getElementsByClass("Row");
        for(int i = 0; i < 5; i++)
        {
            if(i > 0)
            {
                row.append("\t");
            }
            if(i < mostRecentChampTable.size())
            {
                Element champRow = mostRecentChampTable.get(i);
                StringBuilder champData = new StringBuilder();
                champData.append(champRow.getElementsByClass("ChampionName").text());
                champData.append(": ");
                champData.append(champRow.getElementsByClass("GameCount").text());
                champData.append(' ');
                champData.append(champRow.getElementsByClass("WinRatio").text());
                champData.append(' ');
                champData.append(champRow.getElementsByClass("KDA").text());
        
                row.append(champData);
            }
        }
        
        row.append('\n');
        
        return row.toString();
    }
    
    private static String getIndividualLink(Element player)
    {
        return "http:" + player.getElementsByClass("Summoner").get(0).getElementsByAttribute("href").get(0).attr("href");
    }
}
