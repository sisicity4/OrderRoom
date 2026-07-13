import { useState } from 'react'

function CreatePage() {
  const [title, setTitle] = useState('');
  const [date, setDate] = useState('');
  const [memo, setMemo] = useState('');

  return (
    <>
      <h1>ルーム作成画面</h1>

      <input
        type="text"
        placeholder="タイトル"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <input 
      type="date"
      placeholder='日付'
      value={date}
      onChange={(e) =>
        setDate(e.target.value)
      } />

      <textarea
      value={memo}
      placeholder='メモ'
      onChange={(e) =>
        setMemo(e.target.value)
      } />

      <button onClick={() => console.log({title,date,memo})}>作成</button>
      
      
    </>
  );
}
export default CreatePage;