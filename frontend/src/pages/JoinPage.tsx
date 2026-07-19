import { useState } from "react";

function JoinPage() {
  const [name,setName]=useState('');
  const [url,setUrl]=useState('');
  return (
 <div className='flex flex-col gap-4 p-8 bg-[#f0e5cc] min-h-screen'>
      <p className='text-[#7A6B57] tracking-[1em]'>相席</p>
      <p className='text-[#7A6B57] tracking-[1em]'>るーむさんか</p>
      <h1 className='text-4xl font-bold'>ルーム参加</h1>

    <div className='flex self-center py-4'>    {/*名前入力欄*/}
      <p className='border w-fit px-2 mr-2'>名前</p>
      <input
      className='border-b outline-none w-55'
      type="text"
      value={name}
      onChange={(e) => setName(e.target.value)}/>
    </div>

      <hr className='border-t-5' />

    <div> {/*URL入力処理*/}
      <p className='border px-3 w-fit mb-2 text-[#7A6B57]'>伝票番号</p>
       <input
      className='border-b outline-none w-70'
      type="text"
      value={url}
      onChange={(e) => setUrl(e.target.value)} />
    </div>

    <hr className='border-t-5' />

    <div className='flex flex-col'>

      <p className='tracking-[0.8em] text-[#7A6B57]'>相席のうえ</p>
      <p className='tracking-[0.8em] text-[#7A6B57]'>注文票を共有します</p>
       <button
       className='self-center'
       onClick={() => console.log({name,url})}>参加</button>
    </div>



 </div>


  );
}
export default JoinPage;