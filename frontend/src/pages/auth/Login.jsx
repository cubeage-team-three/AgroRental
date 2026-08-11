function Login() {
  return (
    <div className="max-w-sm mx-auto bg-white shadow-md rounded-lg p-6">
      <h2 className="text-2xl font-semibold mb-4 text-center">Login</h2>
      <form className="flex flex-col gap-4">
        <input
          type="email"
          placeholder="Email"
          className="border rounded px-3 py-2"
        />
        <input
          type="password"
          placeholder="Password"
          className="border rounded px-3 py-2"
        />
        <button
          type="submit"
          className="bg-green-700 text-white rounded py-2 hover:bg-green-800"
        >
          Login
        </button>
      </form>
    </div>
  );
}

export default Login;
