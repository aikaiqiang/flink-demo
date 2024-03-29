package com.aikq.flink;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.Collector;


/**
 *  E
 * @author aikq
 * @date 2019年02月22日 14:47
 */
public class SocketTextStreamWordCount {

	public static void main(String[] args) throws Exception {

		// 参数校验
		if (args.length != 2){
			System.out.println("USAGE:\nSocketTextStreamWordCount <hostname> <port>");
			return;
		}

		String hostname = args[0];
		Integer port = Integer.parseInt(args[1]);

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		//获取数据
		DataStream<String> stream = env.socketTextStream(hostname, port);

		// parse the data, group it, window it, and aggregate the counts
		DataStream<WordWithCount> windowCounts = stream
				.flatMap(new FlatMapFunction<String, WordWithCount>() {
					@Override
					public void flatMap(String value, Collector<WordWithCount> out) {
						for (String word : value.split("\\s")) {
							out.collect(new WordWithCount(word, 1L));
						}
					}
				})
				.keyBy("word")
				.timeWindow(Time.seconds(5))
				.reduce(new ReduceFunction<WordWithCount>() {
					@Override
					public WordWithCount reduce(WordWithCount a, WordWithCount b) {
						return new WordWithCount(a.word, a.count + b.count);
					}
				});

		//计数
//		SingleOutputStreamOperator<Tuple2<String, Integer>> sum = stream.flatMap(new LineSplitter())
//				.keyBy(0)
//				.sum(1);
//
//		sum.print();

		// print the results with a single thread, rather than in parallel
		windowCounts.print().setParallelism(1);

		env.execute("Socket Window WordCount");
	}

	public static final class LineSplitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
		@Override
		public void flatMap(String s, Collector<Tuple2<String, Integer>> collector) {
			String[] tokens = s.toLowerCase().split("\\W+");
			for (String token: tokens) {
				if (token.length() > 0) {
					collector.collect(new Tuple2<String, Integer>(token, 1));
				}
			}
		}
	}

	public static class WordWithCount {

		public String word;
		public long count;

		public WordWithCount() {}

		public WordWithCount(String word, long count) {
			this.word = word;
			this.count = count;
		}

		@Override
		public String toString() {
			return word + " : " + count;
		}
	}
}
