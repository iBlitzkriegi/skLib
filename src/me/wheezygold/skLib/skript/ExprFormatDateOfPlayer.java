
Save New Duplicate & Edit Just Text Twitter
1
2
3
4
5
6
7
8
9
10
11
12
13
14
15
16
17
18
19
20
21
22
23
24
25
26
27
28
29
30
31
32
33
34
35
36
37
38
39
40
41
42
43
44
45
46
47
48
49
50
package me.wheezygold.skLib.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.skript.lang.Expression;
import me.wheezygold.skLib.common.JsonReader;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class ExprFormatDateOfPlayer extends SimplePropertyExpression<Player, String>{
	static {
		PropertyExpression.register(ExprFormatDateOfPlayer.class, String.class, "formatted date", "players");
	}
	@Override
	protected String getPropertyName() {
		return "formatted date";
	}

	@Override
	public String convert(Player o) {
		InetAddress rawIp = o.getAddress().getAddress();
		String ip = rawIp.toString().replaceAll("/", "");
		JSONObject jsonParsed = null;
		try {
			jsonParsed = JsonReader.readJsonFromUrl("http://freegeoip.net/json/" + ip);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}
		String jsonValue = (String) jsonParsed.get("time_zone");
		Date date = new Date();
		DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
		df.setTimeZone(TimeZone.getTimeZone(jsonvalue));
		return df.format(date);
	}

	@Override
	public Class<? extends String> getReturnType() {
		return String.class;
	}
}
