/*
BSD License

Copyright(c) 2011, N.Matayoshi All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

・Redistributions of source code must retain the above copyright notice, 
  this list of conditions and the following disclaimer.
・Redistributions in binary form must reproduce the above copyright notice, 
  this list of conditions and the following disclaimer in the documentation 
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
ARE DISCLAIMED. 
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, 
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF 
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package nmtysh.android.app.historycleaner;

import android.content.Context;
import android.database.Cursor;
import android.provider.Browser;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * チェックボックス付きのリストビューを扱うためのクラス
 * 
 * @author nobuho
 */
class CustomCursorAdapter extends SimpleCursorAdapter {
	private Context context;

	public CustomCursorAdapter(Context context, Cursor c, String[] from,
			int[] to) {
		super(context, R.layout.list, c, from, to);
		this.context = context;
	}

	public View getView(int position, View view, ViewGroup parent) {
		Holder holder;

		if (view == null) {
			view = new CustomView(context);
			holder = new Holder();
			holder.url = (TextView) view.findViewById(R.id.url);
			holder.title = (TextView) view.findViewById(R.id.title);
			holder.viewCount = (TextView) view.findViewById(R.id.view_count);

			view.setTag(holder);
		} else {
			holder = (Holder) view.getTag();
		}
		Cursor c = (Cursor) getItem(position);
		c.moveToPosition(position);

		// データの取得と設定 タイトルとURLが同じなら、タイトルに(no title)と設定する
		String title = c.getString(c
				.getColumnIndex(Browser.BookmarkColumns.TITLE));
		String url = c.getString(c
				.getColumnIndex(Browser.BookmarkColumns.URL));

		holder.title.setText(title.equals(url) ? "(no title)" : title);
		holder.url.setText(url);
		holder.viewCount.setText(c.getString(c
				.getColumnIndex(Browser.BookmarkColumns.VISITS)));
		return view;
	}

	private class Holder {
		TextView url;
		TextView title;
		TextView viewCount;
	}
}
// EOF