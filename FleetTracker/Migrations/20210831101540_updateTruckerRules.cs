using Microsoft.EntityFrameworkCore.Migrations;

namespace FleetTracker.Migrations
{
    public partial class updateTruckerRules : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<float>(
                name: "MaxAccel",
                table: "AspNetUsers",
                type: "float",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "MaxSpeed",
                table: "AspNetUsers",
                type: "int",
                nullable: true);

            migrationBuilder.AddColumn<int>(
                name: "MinSpeed",
                table: "AspNetUsers",
                type: "int",
                nullable: true);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "MaxAccel",
                table: "AspNetUsers");

            migrationBuilder.DropColumn(
                name: "MaxSpeed",
                table: "AspNetUsers");

            migrationBuilder.DropColumn(
                name: "MinSpeed",
                table: "AspNetUsers");
        }
    }
}
